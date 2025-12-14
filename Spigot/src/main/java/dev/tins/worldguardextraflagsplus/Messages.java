package dev.tins.worldguardextraflagsplus;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Messages
{
	private static JavaPlugin plugin;
	private static FileConfiguration messages;
	private static File messagesFile;
	private static int messageCooldownSeconds;
	private static final ConcurrentHashMap<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();

	public static void initialize(JavaPlugin plugin)
	{
		Messages.plugin = plugin;
		
		// Get WorldGuard plugin data folder
		File worldGuardDataFolder = plugin.getServer().getPluginManager().getPlugin("WorldGuard").getDataFolder();
		
		// Create messages-wgefp.yml in WorldGuard folder
		messagesFile = new File(worldGuardDataFolder, "messages-wgefp.yml");
		
		// Handle old messages.yml file migration/deletion
		File oldMessagesFile = new File(worldGuardDataFolder, "messages.yml");
		if (oldMessagesFile.exists())
		{
			try
			{
				FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldMessagesFile);
				if (oldConfig.contains("permit-completely-blocked"))
				{
					// Old file contains the deprecated message key, delete it
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
		
		// Copy default messages-wgefp.yml if it doesn't exist
		if (!messagesFile.exists())
		{
			saveDefaultMessages(worldGuardDataFolder);
		}
		
		// Load messages-wgefp.yml
		reloadMessages();
	}

	private static void saveDefaultMessages(File worldGuardDataFolder)
	{
		try
		{
			// Ensure WorldGuard folder exists
			if (!worldGuardDataFolder.exists())
			{
				worldGuardDataFolder.mkdirs();
			}
			
			// Check if messages-wgefp.yml already exists in WorldGuard folder
			if (messagesFile.exists())
			{
				// File already exists, don't overwrite it (admin might have customized it)
				plugin.getLogger().info("messages-wgefp.yml already exists in WorldGuard folder, skipping default copy.");
				return;
			}
			
			// Load default messages from plugin resources
			InputStream defaultStream = plugin.getResource("messages-wgefp.yml");
			if (defaultStream == null)
			{
				plugin.getLogger().warning("Default messages-wgefp.yml not found in plugin resources!");
				return;
			}
			
			// Copy file directly using streams (simpler than loading YAML)
			java.io.FileOutputStream outputStream = new java.io.FileOutputStream(messagesFile);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = defaultStream.read(buffer)) > 0)
			{
				outputStream.write(buffer, 0, length);
			}
			outputStream.close();
			defaultStream.close();
			
			plugin.getLogger().info("Created messages-wgefp.yml in WorldGuard folder: " + messagesFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to save default messages-wgefp.yml", e);
		}
	}

	public static void reloadMessages()
	{
		try
		{
			// Load file without defaults first to check actual file content
			YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(messagesFile);
			
			// Load UTF-8 encoding properly
			InputStream defaultStream = plugin.getResource("messages-wgefp.yml");
			YamlConfiguration defaultConfig = null;
			if (defaultStream != null)
			{
				InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8);
				defaultConfig = YamlConfiguration.loadConfiguration(reader);
			}
			
			boolean needsSave = false;
			String fileContent = null;
			
			// Migrate permit-completely-blocked to disable-completely-blocked
			// Check actual file content, not defaults
			if (fileConfig.contains("permit-completely-blocked", false) && !fileConfig.contains("disable-completely-blocked", false))
			{
				// Read file as text to preserve exact formatting
				fileContent = new String(Files.readAllBytes(messagesFile.toPath()), StandardCharsets.UTF_8);
				String originalContent = fileContent;
				
				// Replace permit-completely-blocked with disable-completely-blocked (preserve formatting)
				fileContent = fileContent.replaceAll("(?m)^(\\s*)#?\\s*permit-completely-blocked\\s*:", "$1disable-completely-blocked:");
				fileContent = fileContent.replaceAll("(?m)^(\\s*)permit-completely-blocked\\s*:", "$1disable-completely-blocked:");
				
				// Also update comment if it exists
				fileContent = fileContent.replaceAll("(?m)^(\\s*)#\\s*Permit completely flag message", "$1# Disable completely flag message");
				
				if (!fileContent.equals(originalContent))
				{
					needsSave = true;
					plugin.getLogger().info("Migrated 'permit-completely-blocked' to 'disable-completely-blocked' in messages-wgefp.yml");
				}
			}
			
			// Add inventory-craft-blocked if missing (check actual file, not defaults)
			if (!fileConfig.contains("inventory-craft-blocked", false))
			{
				// Read file as text if not already read
				if (fileContent == null)
				{
					fileContent = new String(Files.readAllBytes(messagesFile.toPath()), StandardCharsets.UTF_8);
				}
				String originalContent = fileContent;
				
				String defaultValue = defaultConfig != null ? defaultConfig.getString("inventory-craft-blocked") : "&cHey! &7You can not craft items in your inventory here!";
				if (defaultValue != null)
				{
					// Detect quote style from existing messages (prefer double quotes, fallback to single)
					String quoteChar = "\"";
					if (fileContent.contains("permit-workbenches-blocked:") || fileContent.contains("disable-completely-blocked:"))
					{
						// Check what quote style is used
						String sampleLine = fileContent.contains("permit-workbenches-blocked:") 
							? fileContent.substring(fileContent.indexOf("permit-workbenches-blocked:"))
							: fileContent.substring(fileContent.indexOf("disable-completely-blocked:"));
						int firstQuote = sampleLine.indexOf('\'');
						int firstDoubleQuote = sampleLine.indexOf('"');
						if (firstQuote != -1 && (firstDoubleQuote == -1 || firstQuote < firstDoubleQuote))
						{
							quoteChar = "'";
						}
					}
					
					// Find the last message entry and add the new one after it
					// Look for the last message line (permit-workbenches-blocked or disable-completely-blocked)
					if (fileContent.contains("permit-workbenches-blocked:") || fileContent.contains("disable-completely-blocked:"))
					{
						// Add after the last message entry
						String newEntry = "\n# Inventory craft flag message\ninventory-craft-blocked: " + quoteChar + defaultValue + quoteChar;
						
						// Find the last occurrence of a message entry and add after it
						int lastIndex = Math.max(
							fileContent.lastIndexOf("permit-workbenches-blocked:"),
							fileContent.lastIndexOf("disable-completely-blocked:")
						);
						
						if (lastIndex != -1)
						{
							// Find the end of that line (handle wrapped lines by finding the next non-indented line or end of file)
							int lineEnd = fileContent.indexOf('\n', lastIndex);
							if (lineEnd == -1)
							{
								lineEnd = fileContent.length();
							}
							else
							{
								lineEnd++; // Include the newline
								// Skip any wrapped continuation lines (lines that start with spaces after the value)
								while (lineEnd < fileContent.length())
								{
									int nextLineStart = lineEnd;
									// Skip whitespace
									while (nextLineStart < fileContent.length() && (fileContent.charAt(nextLineStart) == ' ' || fileContent.charAt(nextLineStart) == '\t'))
									{
										nextLineStart++;
									}
									// If next line starts with a quote or is empty/comment, it's a continuation
									if (nextLineStart < fileContent.length() && 
										(fileContent.charAt(nextLineStart) == '"' || 
										 fileContent.charAt(nextLineStart) == '\'' ||
										 fileContent.charAt(nextLineStart) == '#' ||
										 fileContent.charAt(nextLineStart) == '\n' ||
										 fileContent.charAt(nextLineStart) == '\r'))
									{
										// This is a continuation line, skip it
										int nextNewline = fileContent.indexOf('\n', nextLineStart);
										if (nextNewline == -1)
										{
											lineEnd = fileContent.length();
											break;
										}
										lineEnd = nextNewline + 1;
									}
									else
									{
										// This is a new key, stop here
										break;
									}
								}
							}
							
							// Insert the new entry
							fileContent = fileContent.substring(0, lineEnd) + newEntry + fileContent.substring(lineEnd);
						}
						else
						{
							// Fallback: append at the end
							fileContent = fileContent + newEntry;
						}
					}
					else
					{
						// Fallback: append at the end
						fileContent = fileContent + "\n# Inventory craft flag message\ninventory-craft-blocked: " + quoteChar + defaultValue + quoteChar;
					}
					
					if (!fileContent.equals(originalContent))
					{
						needsSave = true;
						plugin.getLogger().info("Added missing 'inventory-craft-blocked' key to messages-wgefp.yml");
					}
				}
			}
			
			// Save if changes were made (using text-based save to preserve formatting)
			if (needsSave && fileContent != null)
			{
				Files.write(messagesFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
				plugin.getLogger().info("Updated messages-wgefp.yml with new keys");
			}
			
			// Now load with defaults for actual use
			messages = YamlConfiguration.loadConfiguration(messagesFile);
			if (defaultConfig != null)
			{
				messages.setDefaults(defaultConfig);
			}
			
			// Load message cooldown (default: 3 seconds, 0 = no cooldown)
			messageCooldownSeconds = messages.getInt("send-message-cooldown", 3);
			if (messageCooldownSeconds < 0)
			{
				messageCooldownSeconds = 0;
			}
			
			// Clear cooldowns when reloading messages
			clearAllCooldowns();
			
			plugin.getLogger().info("Loaded messages from: " + messagesFile.getAbsolutePath());
			plugin.getLogger().info("Message cooldown: " + (messageCooldownSeconds > 0 ? messageCooldownSeconds + " seconds" : "disabled"));
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to load messages-wgefp.yml", e);
			// Fallback: use in-memory configuration
			messages = new YamlConfiguration();
			messageCooldownSeconds = 3; // Default fallback
		}
	}

	/**
	 * Gets a message from the configuration and translates color codes.
	 * Supports placeholders: {key} will be replaced with values
	 * Returns null if message is empty (disabled)
	 */
	public static String getMessage(String key, String... replacements)
	{
		String message = messages.getString(key, "&cMessage not found: " + key);
		
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
		return messages.getString(key, "");
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

	public static File getMessagesFile()
	{
		return messagesFile;
	}

	public static int getMessageCooldownSeconds()
	{
		return messageCooldownSeconds;
	}
}

