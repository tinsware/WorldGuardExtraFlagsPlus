package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.Handler;
import com.sk89q.worldguard.session.Session;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class EntryLevelFlagHandler extends Handler
{
	public static final Factory FACTORY(Plugin plugin)
	{
		return new Factory(plugin);
	}

	public static class Factory extends Handler.Factory<EntryLevelFlagHandler>
	{
		private final Plugin plugin;

		public Factory(Plugin plugin)
		{
			this.plugin = plugin;
		}

		@Override
		public EntryLevelFlagHandler create(Session session)
		{
			return new EntryLevelFlagHandler(this.plugin, session);
		}
	}

	private final Plugin plugin;
	
	// Cache: player UUID -> (placeholder -> (value, timestamp))
	private final ConcurrentHashMap<String, PlaceholderCacheEntry> placeholderCache = new ConcurrentHashMap<>();
	private static final long CACHE_DURATION_MS = TimeUnit.SECONDS.toMillis(30);
	private static final int PLACEHOLDER_CACHE_MAX_SIZE = 256;

	private static class PlaceholderCacheEntry
	{
		private final Integer value;
		private final long timestamp;
		
		public PlaceholderCacheEntry(Integer value, long timestamp)
		{
			this.value = value;
			this.timestamp = timestamp;
		}
		
		public Integer getValue()
		{
			return value;
		}
		
		public long getTimestamp()
		{
			return timestamp;
		}
		
		public boolean isExpired()
		{
			return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
		}
	}

	protected EntryLevelFlagHandler(Plugin plugin, Session session)
	{
		super(session);
		this.plugin = plugin;
	}

	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		// Check if player has bypass
		if (this.getSession().getManager().hasBypass(player, (World) to.getExtent()))
		{
			return true; // Allow entry if player has bypass
		}

		// Check entry-min-level flag
		// Format: "<threshold> <source>" where source is "XP" or a placeholder like "%battlepass_tier%"
		// Example: "10 XP" or "10 %battlepass_tier%"
		String minLevelValue = toSet.queryValue(player, Flags.ENTRY_MIN_LEVEL);
		if (minLevelValue != null && !minLevelValue.isEmpty())
		{
			ParsedFlagValue parsed = parseFlagValue(minLevelValue);
			if (parsed == null)
			{
				// Invalid format - allow entry (don't break experience)
				return true;
			}
			
			Integer playerValue = getPlayerValue(player, parsed.source);
			if (playerValue == null)
			{
				// Invalid source or cannot get value - allow entry (don't break experience)
				return true;
			}
			
			if (playerValue < parsed.threshold)
			{
				// Player value is below minimum, deny entry
				sendDeniedMessage(((BukkitPlayer) player).getPlayer(), "entry-min-level", parsed.threshold, playerValue);
				return false; // Deny entry
			}
		}

		// Check entry-max-level flag
		// Format: "<threshold> <source>" where source is "XP" or a placeholder like "%battlepass_tier%"
		// Example: "70 XP" or "70 %armor_durability_left_helmet%"
		String maxLevelValue = toSet.queryValue(player, Flags.ENTRY_MAX_LEVEL);
		if (maxLevelValue != null && !maxLevelValue.isEmpty())
		{
			ParsedFlagValue parsed = parseFlagValue(maxLevelValue);
			if (parsed == null)
			{
				// Invalid format - allow entry (don't break experience)
				return true;
			}
			
			Integer playerValue = getPlayerValue(player, parsed.source);
			if (playerValue == null)
			{
				// Invalid source or cannot get value - allow entry (don't break experience)
				return true;
			}
			
			if (playerValue > parsed.threshold)
			{
				// Player value is above maximum, deny entry
				sendDeniedMessage(((BukkitPlayer) player).getPlayer(), "entry-max-level", parsed.threshold, playerValue);
				return false; // Deny entry
			}
		}

		return true; // Allow entry
	}
	
	/**
	 * Parsed flag value containing threshold and source.
	 */
	private static class ParsedFlagValue
	{
		final int threshold;
		final String source;
		
		ParsedFlagValue(int threshold, String source)
		{
			this.threshold = threshold;
			this.source = source;
		}
	}
	
	/**
	 * Parses flag value in format: "<threshold> <source>"
	 * Returns null if invalid format.
	 */
	private ParsedFlagValue parseFlagValue(String flagValue)
	{
		if (flagValue == null || flagValue.trim().isEmpty())
		{
			return null;
		}
		
		String trimmed = flagValue.trim();
		String[] parts = trimmed.split("\\s+", 2);
		
		if (parts.length != 2)
		{
			return null;
		}
		
		// Parse threshold (first argument)
		int threshold;
		try
		{
			threshold = Integer.parseInt(parts[0].trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
		
		// Get source (second argument)
		String source = parts[1].trim();
		
		return new ParsedFlagValue(threshold, source);
	}
	
	/**
	 * Gets player value from source.
	 * Source can be "XP" or a PlaceholderAPI placeholder.
	 * Returns null if source is invalid or value cannot be retrieved.
	 */
	private Integer getPlayerValue(LocalPlayer localPlayer, String source)
	{
		if (source == null || source.trim().isEmpty())
		{
			return null;
		}
		
		String trimmed = source.trim();
		
		// Check if source is "XP"
		if (trimmed.equalsIgnoreCase("XP"))
		{
			Player bukkitPlayer = ((BukkitPlayer) localPlayer).getPlayer();
			if (!bukkitPlayer.isOnline())
			{
				return null;
			}
			return bukkitPlayer.getLevel(); // Minecraft XP level
		}
		
		// Check if source is a placeholder (starts with % and ends with %)
		if (trimmed.startsWith("%") && trimmed.endsWith("%") && trimmed.length() > 2)
		{
			// It's a placeholder - evaluate via PlaceholderAPI
			return getPlayerPlaceholderValue(localPlayer, trimmed);
		}
		
		// Invalid source format
		return null;
	}

	/**
	 * Gets player value from a PlaceholderAPI placeholder.
	 * Uses caching to reduce API calls.
	 * Returns null if PlaceholderAPI is not available or placeholder is invalid.
	 */
	private Integer getPlayerPlaceholderValue(LocalPlayer localPlayer, String placeholder)
	{
		if (placeholder == null || placeholder.trim().isEmpty())
		{
			return null;
		}

		Player bukkitPlayer = ((BukkitPlayer) localPlayer).getPlayer();
		
		if (!bukkitPlayer.isOnline())
		{
			return null;
		}

		String trimmed = placeholder.trim();

		// Check if it's a placeholder (starts with % and ends with %)
		if (!trimmed.startsWith("%") || !trimmed.endsWith("%") || trimmed.length() <= 2)
		{
			return null;
		}

		// Check cache
		String cacheKey = bukkitPlayer.getUniqueId().toString() + trimmed;
		PlaceholderCacheEntry cached = placeholderCache.get(cacheKey);
		
		if (cached != null && !cached.isExpired())
		{
			return cached.getValue();
		}

		// Check if PlaceholderAPI is available
		if (!isPlaceholderAPIAvailable())
		{
			return null;
		}

		// Get placeholder value from PlaceholderAPI using reflection (soft dependency)
		String placeholderValue = getPlaceholderValue(bukkitPlayer, trimmed);
		
		// Parse as integer
		Integer parsedValue = parseInteger(placeholderValue);

		// Cache the result
		if (parsedValue != null)
		{
			if (placeholderCache.size() >= PLACEHOLDER_CACHE_MAX_SIZE)
			{
				placeholderCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
			}
			placeholderCache.put(cacheKey, new PlaceholderCacheEntry(parsedValue, System.currentTimeMillis()));
		}

		return parsedValue;
	}

	/**
	 * Checks if PlaceholderAPI is available on the server.
	 */
	private boolean isPlaceholderAPIAvailable()
	{
		return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
	}

	/**
	 * Gets placeholder value using PlaceholderAPI reflection.
	 */
	private String getPlaceholderValue(Player player, String placeholder)
	{
		if (!isPlaceholderAPIAvailable())
		{
			return "";
		}

		try
		{
			// Use reflection to call PlaceholderAPI.setPlaceholders() (soft dependency)
			Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			java.lang.reflect.Method setPlaceholdersMethod = placeholderAPIClass.getMethod("setPlaceholders", Player.class, String.class);
			return (String) setPlaceholdersMethod.invoke(null, player, placeholder);
		}
		catch (Exception e)
		{
			// PlaceholderAPI not available or error occurred
			return "";
		}
	}

	/**
	 * Parses a string value to an integer, handling numeric extraction.
	 */
	private Integer parseInteger(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return null;
		}

		try
		{
			// Remove any non-numeric characters (for safety) but preserve negative sign
			String numericOnly = value.trim().replaceAll("[^0-9-]", "");
			if (!numericOnly.isEmpty() && !numericOnly.equals("-"))
			{
				return Integer.parseInt(numericOnly);
			}
		}
		catch (NumberFormatException e)
		{
			// Invalid format
		}

		return null;
	}

	private void sendDeniedMessage(Player player, String flagType, Integer requiredLevel, Integer playerLevel)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}

		// Determine message key and replacements
		final String messageKey;
		final String[] replacements;
		if ("entry-min-level".equals(flagType))
		{
			messageKey = "entry-min-level-denied";
			replacements = new String[]{
				"required", String.valueOf(requiredLevel),
				"current", playerLevel != null ? String.valueOf(playerLevel) : "?"
			};
		}
		else // entry-max-level
		{
			messageKey = "entry-max-level-denied";
			replacements = new String[]{
				"required", String.valueOf(requiredLevel),
				"current", playerLevel != null ? String.valueOf(playerLevel) : "?"
			};
		}

		// Send message with cooldown using FoliaLib scheduler (runs on entity thread)
		WorldGuardUtils.getScheduler().runAtEntity(player, task -> {
			if (player.isOnline())
			{
				sendMessageWithCooldown(player, messageKey, replacements);
			}
		});
	}
	
	/**
	 * Sends a message to a player with cooldown using reflection (WG module can't depend on Spigot module).
	 * Uses Messages.sendMessageWithCooldown() from Spigot module.
	 */
	private void sendMessageWithCooldown(Player player, String key, String... replacements)
	{
		try
		{
			// Use reflection to call Messages.sendMessageWithCooldown() from Spigot module
			Class<?> messagesClass = Class.forName("dev.tins.worldguardextraflagsplus.Messages");
			java.lang.reflect.Method sendMessageMethod = messagesClass.getMethod("sendMessageWithCooldown", 
				org.bukkit.entity.Player.class, String.class, String[].class);
			sendMessageMethod.invoke(null, player, key, replacements);
		}
		catch (Exception e)
		{
			// Fallback to sending message directly without cooldown if reflection fails
			String message;
			if ("entry-min-level-denied".equals(key))
			{
				// Extract required and current from replacements array
				String required = replacements.length > 1 ? replacements[1] : "?";
				String current = replacements.length > 3 ? replacements[3] : "?";
				message = org.bukkit.ChatColor.RED + "Your level (" + current + ") is low to enter this area. " + 
				       org.bukkit.ChatColor.GRAY + "Min: " + required;
			}
			else if ("entry-max-level-denied".equals(key))
			{
				// Extract required and current from replacements array
				String required = replacements.length > 1 ? replacements[1] : "?";
				String current = replacements.length > 3 ? replacements[3] : "?";
				message = org.bukkit.ChatColor.RED + "Your level (" + current + ") is so high to enter this area. " + 
				       org.bukkit.ChatColor.GRAY + "Max: " + required;
			}
			else
			{
				message = org.bukkit.ChatColor.RED + "Message not found: " + key;
			}
			
			if (player.isOnline())
			{
				player.sendMessage(message);
			}
		}
	}

}

