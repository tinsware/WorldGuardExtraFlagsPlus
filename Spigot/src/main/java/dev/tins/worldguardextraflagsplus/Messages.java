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
			messages = YamlConfiguration.loadConfiguration(messagesFile);
			
			// Load UTF-8 encoding properly
			InputStream defaultStream = plugin.getResource("messages-wgefp.yml");
			if (defaultStream != null)
			{
				InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8);
				YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
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

