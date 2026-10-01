package dev.tins.worldguardextraflagsplus;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.tins.worldguardextraflagsplus.config.PluginMessages;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Messages manager using ConfigLib.
 * Maintains static methods for backward compatibility.
 * Preserves special features: empty string = disabled, placeholders, cooldown.
 */
public class Messages
{
	private static JavaPlugin plugin;
	private static PluginMessages messages;
	private static Path messagesFile;
	private static int messageCooldownSeconds;
	private static final ConcurrentHashMap<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
	
	// Map of kebab-case keys to message values (for getMessage() compatibility)
	private static final Map<String, String> messageMap = new HashMap<>();
	
	// ConfigLib properties with kebab-case formatter
	private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
		.setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
		.header(PluginMessages.MESSAGES_HEADER)
		.build();
	
	public static void initialize(JavaPlugin plugin)
	{
		Messages.plugin = plugin;
		
		// Get WorldGuard plugin data folder
		File worldGuardDataFolder = plugin.getServer().getPluginManager().getPlugin("WorldGuard").getDataFolder();
		
		// Create messages-wgefp.yml in WorldGuard folder
		messagesFile = worldGuardDataFolder.toPath().resolve("messages-wgefp.yml");
		
		// Handle old messages.yml file migration/deletion
		File oldMessagesFile = new File(worldGuardDataFolder, "messages.yml");
		if (oldMessagesFile.exists())
		{
			try
			{
				YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldMessagesFile);
				if (oldConfig.contains("permit-completely-blocked"))
				{
					if (oldMessagesFile.delete())
					{
						plugin.getLogger().info("Deleted old messages.yml file (migrated to messages-wgefp.yml)");
					}
					else
					{
						plugin.getLogger().warning("Failed to delete old messages.yml file. Please delete it manually.");
					}
				}
			}
			catch (Exception e)
			{
				plugin.getLogger().log(Level.WARNING, "Error checking old messages.yml file: " + e.getMessage(), e);
			}
		}
		
		// Load messages
		reloadMessages();
	}
	
	public static void reloadMessages()
	{
		reloadMessages(false);
	}

	private static void reloadMessages(boolean recoveryAttempted)
	{
		try
		{
			// Ensure WorldGuard folder exists
			if (!messagesFile.getParent().toFile().exists())
			{
				messagesFile.getParent().toFile().mkdirs();
			}

			// Quarantine oversize files before any read/parse (SnakeYAML safety limit is much higher)
			WgefpYamlFileGuard.checkAndQuarantineOversize(messagesFile, plugin.getLogger());
			
			// Pre-migration: migrate permit-completely-blocked to disable-completely-blocked
			// Do this BEFORE ConfigLib loads to preserve user values
			if (messagesFile.toFile().exists())
			{
				try
				{
					String fileContent = new String(Files.readAllBytes(messagesFile), StandardCharsets.UTF_8);
					String originalContent = fileContent;
					
					// Check if file contains old key but not new key
					if (fileContent.contains("permit-completely-blocked:") && !fileContent.contains("disable-completely-blocked:"))
					{
						// Replace permit-completely-blocked with disable-completely-blocked (preserve formatting)
						fileContent = fileContent.replaceAll("(?m)^(\\s*)#?\\s*permit-completely-blocked\\s*:", "$1disable-completely-blocked:");
						fileContent = fileContent.replaceAll("(?m)^(\\s*)permit-completely-blocked\\s*:", "$1disable-completely-blocked:");
						
						// Also update comment if it exists
						fileContent = fileContent.replaceAll("(?m)^(\\s*)#\\s*Permit completely flag message", "$1# Disable completely flag message");
						
						if (!fileContent.equals(originalContent))
						{
							WgefpYamlFileGuard.writeAtomically(messagesFile, fileContent);
							plugin.getLogger().info("Migrated 'permit-completely-blocked' to 'disable-completely-blocked' in messages-wgefp.yml");
						}
					}
				}
				catch (Exception e)
				{
					plugin.getLogger().log(Level.WARNING, "Error during pre-migration: " + e.getMessage(), e);
				}
			}
			
			// Load or update messages using ConfigLib
			messages = YamlConfigurations.update(messagesFile, PluginMessages.class, PROPERTIES);
			
			// Post-process to fix line wrapping in string values (keep strings on single line)
			// This must happen AFTER ConfigLib writes, as ConfigLib/SnakeYAML wraps long strings
			fixLineWrapping(messagesFile);
			
			// Reload the fixed file (use load instead of update to avoid re-writing)
			messages = YamlConfigurations.load(messagesFile, PluginMessages.class, PROPERTIES);
			
			// Build message map for getMessage() compatibility (kebab-case keys)
			buildMessageMap();
			
			// Load message cooldown
			messageCooldownSeconds = messages.getSendMessageCooldown();
			if (messageCooldownSeconds < 0)
			{
				messageCooldownSeconds = 0;
			}
			
			// Clear cooldowns when reloading messages
			clearAllCooldowns();
			
			Config.logStartupInfo("Loaded messages from: " + messagesFile.toAbsolutePath());
			Config.logStartupInfo("Message cooldown: " + (messageCooldownSeconds > 0 ? messageCooldownSeconds + " seconds" : "disabled"));
		}
		catch (de.exlll.configlib.ConfigurationException e)
		{
			if (!recoveryAttempted && WgefpYamlFileGuard.isRecoverableYamlError(e))
			{
				try
				{
					WgefpYamlFileGuard.quarantineCorrupt(messagesFile, plugin.getLogger());
					plugin.getLogger().warning("Auto-recovery: quarantined messages-wgefp.yml and retrying with fresh defaults.");
					reloadMessages(true);
					return;
				}
				catch (Exception recoveryError)
				{
					plugin.getLogger().log(Level.WARNING, "Auto-recovery failed: " + recoveryError.getMessage(), recoveryError);
				}
			}

			String errorMsg = WgefpYamlFileGuard.buildYamlErrorMessage(e, "messages-wgefp.yml");
			WgefpYamlFileGuard.logYamlLoadFailure(plugin.getLogger(), messagesFile, "messages-wgefp.yml", errorMsg, recoveryAttempted);
			disablePluginWithDefaultMessages();
		}
		catch (Exception e)
		{
			if (!recoveryAttempted && WgefpYamlFileGuard.isRecoverableGenericError(e))
			{
				try
				{
					WgefpYamlFileGuard.quarantineCorrupt(messagesFile, plugin.getLogger());
					plugin.getLogger().warning("Auto-recovery: quarantined messages-wgefp.yml and retrying with fresh defaults.");
					reloadMessages(true);
					return;
				}
				catch (Exception recoveryError)
				{
					plugin.getLogger().log(Level.WARNING, "Auto-recovery failed: " + recoveryError.getMessage(), recoveryError);
				}
			}

			String errorMsg = e.getMessage();
			if (errorMsg != null && errorMsg.length() > 100)
			{
				errorMsg = errorMsg.substring(0, 100) + "...";
			}
			WgefpYamlFileGuard.logYamlLoadFailure(plugin.getLogger(), messagesFile, "messages-wgefp.yml",
					"Failed to load messages-wgefp.yml: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName()),
					recoveryAttempted);
			disablePluginWithDefaultMessages();
		}
	}

	private static void disablePluginWithDefaultMessages()
	{
		plugin.getServer().getPluginManager().disablePlugin(plugin);
		messages = new PluginMessages();
		buildMessageMap();
		messageCooldownSeconds = 3;
	}
	
	/**
	 * Builds a map of kebab-case keys to message values for getMessage() compatibility.
	 * This allows getMessage() to work with kebab-case keys while ConfigLib uses camelCase fields.
	 */
	private static void buildMessageMap()
	{
		messageMap.clear();
		
		if (messages == null)
		{
			return;
		}
		
		try
		{
			// Get all fields from PluginMessages class
			Field[] fields = PluginMessages.class.getDeclaredFields();
			
			for (Field field : fields)
			{
				// Skip static fields and cooldown field
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
					field.getName().equals("sendMessageCooldown"))
				{
					continue;
				}
				
				field.setAccessible(true);
				Object value = field.get(messages);
				
				if (value instanceof String)
				{
					// Convert camelCase field name to kebab-case key
					String kebabKey = NameFormatters.LOWER_KEBAB_CASE.format(field.getName());
					messageMap.put(kebabKey, (String) value);
				}
			}
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.WARNING, "Error building message map: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Gets a message from the configuration and translates color codes.
	 * Supports placeholders: {key} will be replaced with values
	 * Returns null if message is empty (disabled)
	 */
	public static String getMessage(String key, String... replacements)
	{
		// Get message from map (kebab-case key)
		String message = messageMap.getOrDefault(key, "&cMessage not found: " + key);
		
		// If message is empty string, return null (message disabled)
		if (message == null || message.trim().isEmpty())
		{
			return null;
		}
		
		// Replace placeholders
		for (int i = 0; i < replacements.length; i += 2)
		{
			if (i + 1 < replacements.length)
			{
				message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
			}
		}
		
		// Translate color codes
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	
	/**
	 * Gets a raw message from the configuration without color translation.
	 * Useful for combining multiple messages.
	 */
	public static String getRawMessage(String key)
	{
		return messageMap.getOrDefault(key, "");
	}
	
	/**
	 * Sends a message to a player with cooldown check.
	 * If cooldown is enabled and not expired, the message won't be sent.
	 * Returns true if message was sent, false if blocked by cooldown.
	 */
	public static boolean sendMessageWithCooldown(Player player, String key, String... replacements)
	{
		if (player == null || !player.isOnline())
		{
			return false;
		}

		// Get message
		String message = getMessage(key, replacements);
		if (message == null)
		{
			return false; // Message disabled
		}

		// Check cooldown if enabled
		if (messageCooldownSeconds > 0)
		{
			UUID playerId = player.getUniqueId();
			long currentTime = System.currentTimeMillis();
			long cooldownMillis = messageCooldownSeconds * 1000L;

			Long lastMessageTime = messageCooldowns.get(playerId);
			if (lastMessageTime != null)
			{
				long timeSinceLastMessage = currentTime - lastMessageTime;
				if (timeSinceLastMessage < cooldownMillis)
				{
					// Still in cooldown, don't send message
					return false;
				}
			}

			// Update cooldown timestamp
			messageCooldowns.put(playerId, currentTime);
		}

		// Send message
		player.sendMessage(message);
		return true;
	}
	
	/**
	 * Clears the message cooldown for a specific player.
	 * Useful for testing or admin commands.
	 */
	public static void clearCooldown(Player player)
	{
		if (player != null)
		{
			messageCooldowns.remove(player.getUniqueId());
		}
	}
	
	/**
	 * Clears all message cooldowns.
	 * Useful when reloading messages.
	 */
	public static void clearAllCooldowns()
	{
		messageCooldowns.clear();
	}
	
	private static boolean fileHasWrappedQuotedStrings(String[] lines)
	{
		for (String line : lines)
		{
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains(":"))
			{
				continue;
			}
			String valuePart = trimmed.substring(trimmed.indexOf(':') + 1).trim();
			if (hasUnclosedYamlQuote(valuePart))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean hasUnclosedYamlQuote(String valuePart)
	{
		if (valuePart.startsWith("'"))
		{
			return !isSingleQuotedScalarClosed(valuePart);
		}
		if (valuePart.startsWith("\""))
		{
			return !isDoubleQuotedScalarClosed(valuePart);
		}
		return false;
	}

	private static boolean isSingleQuotedScalarClosed(String valuePart)
	{
		for (int i = 1; i < valuePart.length(); i++)
		{
			if (valuePart.charAt(i) == '\'')
			{
				if (i + 1 < valuePart.length() && valuePart.charAt(i + 1) == '\'')
				{
					i++;
					continue;
				}
				return i == valuePart.length() - 1;
			}
		}
		return false;
	}

	private static boolean isDoubleQuotedScalarClosed(String valuePart)
	{
		for (int i = 1; i < valuePart.length(); i++)
		{
			char c = valuePart.charAt(i);
			if (c == '\\')
			{
				i++;
				continue;
			}
			if (c == '"')
			{
				return i == valuePart.length() - 1;
			}
		}
		return false;
	}

	private static int findYamlClosingQuoteIndex(String text, char quoteChar)
	{
		if (quoteChar == '\'')
		{
			for (int i = 0; i < text.length(); i++)
			{
				if (text.charAt(i) == '\'')
				{
					if (i + 1 < text.length() && text.charAt(i + 1) == '\'')
					{
						i++;
						continue;
					}
					return i;
				}
			}
			return -1;
		}

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c == '\\')
			{
				i++;
				continue;
			}
			if (c == '"')
			{
				return i;
			}
		}
		return -1;
	}

	private static String unescapeCollectedYamlValue(String raw, char sourceQuote)
	{
		if (sourceQuote == '\'')
		{
			return raw.replace("''", "'");
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < raw.length(); i++)
		{
			char c = raw.charAt(i);
			if (c == '\\' && i + 1 < raw.length())
			{
				sb.append(raw.charAt(i + 1));
				i++;
			}
			else
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/** Prefer double quotes so apostrophes (e.g. can't) need no YAML escaping. */
	private static String formatYamlQuotedString(String value)
	{
		if (!value.contains("\"") && !value.contains("\\"))
		{
			return "\"" + value + "\"";
		}
		if (!value.contains("'"))
		{
			return "'" + value.replace("'", "''") + "'";
		}

		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			if (c == '\\' || c == '"')
			{
				sb.append('\\');
			}
			sb.append(c);
		}
		sb.append('"');
		return sb.toString();
	}

	/**
	 * Fixes line wrapping in YAML string values to keep them on a single line.
	 * This prevents ConfigLib/SnakeYAML from wrapping long strings across multiple lines.
	 * Uses a simple line-by-line parser to detect and fix wrapped strings.
	 */
	private static void fixLineWrapping(Path file)
	{
		try
		{
			byte[] originalBytes = Files.readAllBytes(file);
			String content = new String(originalBytes, StandardCharsets.UTF_8);
			String[] lines = content.split("\n", -1); // Keep trailing empty lines

			if (!fileHasWrappedQuotedStrings(lines))
			{
				return;
			}

			StringBuilder fixed = new StringBuilder();
			
			for (int i = 0; i < lines.length; i++)
			{
				String line = lines[i];
				String trimmed = line.trim();
				
				// Skip empty lines and comments
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
				{
					fixed.append(line);
					if (i < lines.length - 1)
					{
						fixed.append("\n");
					}
					continue;
				}
				
				// Check if this is a key-value line
				if (trimmed.contains(":"))
				{
					int colonIndex = trimmed.indexOf(':');
					String key = trimmed.substring(0, colonIndex).trim();
					String valuePart = trimmed.substring(colonIndex + 1).trim();
					
					if (hasUnclosedYamlQuote(valuePart))
					{
						char quoteChar = valuePart.charAt(0);
						StringBuilder fullValue = new StringBuilder();
						fullValue.append(valuePart.substring(1));
						
						// Calculate base indent (for continuation lines)
						int baseIndent = line.length() - line.trim().length();
						
						// Look ahead for continuation lines
						int j = i + 1;
						for (; j < lines.length; j++)
						{
							String nextLine = lines[j];
							String nextTrimmed = nextLine.trim();
							
							// Stop if we hit a comment or empty line (not part of the string)
							if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("#"))
							{
								break;
							}
							
							// Check if this line is indented (continuation)
							int nextIndent = nextLine.length() - nextLine.trim().length();
							if (nextIndent > baseIndent)
							{
								String continuation = nextTrimmed;
								int closingQuoteIndex = findYamlClosingQuoteIndex(continuation, quoteChar);

								if (closingQuoteIndex >= 0)
								{
									fullValue.append(" ").append(continuation, 0, closingQuoteIndex);
									i = j;
									break;
								}

								fullValue.append(" ").append(continuation);
								i = j;
							}
							else
							{
								// Not a continuation, stop
								break;
							}
						}
						
						String finalValue = unescapeCollectedYamlValue(
								fullValue.toString().replaceAll("\\s+", " ").trim(),
								quoteChar);
						
						// Write the fixed line
						// For message keys (root level), there should be no indentation
						// Use the original line's indentation, but if it's minimal (0-1 spaces), use 0
						int originalIndent = line.length() - line.trim().length();
						// Root-level keys should have no indent, so if indent is small, use 0
						int keyIndent = (originalIndent <= 1) ? 0 : originalIndent;
						
						String indent = " ".repeat(keyIndent);
						fixed.append(indent).append(key).append(": ").append(formatYamlQuotedString(finalValue));
					}
					else
					{
						// Normal line, write as-is
						fixed.append(line);
					}
				}
				else
				{
					// Not a key-value line, write as-is
					fixed.append(line);
				}
				
				if (i < lines.length - 1)
				{
					fixed.append("\n");
				}
			}
			
			byte[] fixedBytes = fixed.toString().getBytes(StandardCharsets.UTF_8);
			if (java.util.Arrays.equals(originalBytes, fixedBytes))
			{
				return;
			}

			WgefpYamlFileGuard.writeAtomically(file, new String(fixedBytes, StandardCharsets.UTF_8));
		}
		catch (Exception e)
		{
			// Log but don't fail - this is just a formatting fix
			if (plugin != null)
			{
				plugin.getLogger().log(Level.WARNING, "Could not fix line wrapping in messages file: " + e.getMessage(), e);
			}
		}
	}
	
	public static File getMessagesFile()
	{
		return messagesFile != null ? messagesFile.toFile() : null;
	}
	
	public static int getMessageCooldownSeconds()
	{
		return messageCooldownSeconds;
	}
}
