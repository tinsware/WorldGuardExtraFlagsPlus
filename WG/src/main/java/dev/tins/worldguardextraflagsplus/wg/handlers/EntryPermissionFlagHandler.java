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

/**
 * Handler for entry-permission and entry-deny-permission flags.
 * 
 * entry-permission: Allows only players with the given permission to enter
 * entry-deny-permission: Denies entry to players with the given permission
 * 
 * If player has both permissions, entry-permission takes priority.
 */
public class EntryPermissionFlagHandler extends Handler
{
	public static final Factory FACTORY(Plugin plugin)
	{
		return new Factory(plugin);
	}

	public static class Factory extends Handler.Factory<EntryPermissionFlagHandler>
	{
		private final Plugin plugin;

		public Factory(Plugin plugin)
		{
			this.plugin = plugin;
		}

		@Override
		public EntryPermissionFlagHandler create(Session session)
		{
			return new EntryPermissionFlagHandler(this.plugin, session);
		}
	}

	private final Plugin plugin;

	protected EntryPermissionFlagHandler(Plugin plugin, Session session)
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

		// Get the Bukkit player for permission checking
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();
		if (bukkitPlayer == null || !bukkitPlayer.isOnline())
		{
			return true; // Allow if player is offline or not found
		}

		// Check entry-permission flag (positive permission requirement)
		// If set, only players with this permission can enter
		String entryPermission = toSet.queryValue(player, Flags.ENTRY_PERMISSION);
		if (entryPermission != null && !entryPermission.isEmpty())
		{
			if (!bukkitPlayer.hasPermission(entryPermission))
			{
				// Player doesn't have the required permission, deny entry
				sendDeniedMessage(player, bukkitPlayer, toSet, "entry-permission", entryPermission);
				return false; // Deny entry
			}
		}

		// Check entry-deny-permission flag (negative permission - denies entry)
		// If set, players with this permission cannot enter (unless entry-permission allowed them above)
		String entryDenyPermission = toSet.queryValue(player, Flags.ENTRY_DENY_PERMISSION);
		if (entryDenyPermission != null && !entryDenyPermission.isEmpty())
		{
			// Only apply deny if player doesn't have entry-permission that allowed them
			if (entryPermission == null || entryPermission.isEmpty())
			{
				if (bukkitPlayer.hasPermission(entryDenyPermission))
				{
					// Player has the deny permission and no allow permission, deny entry
					sendDeniedMessage(player, bukkitPlayer, toSet, "entry-deny-permission", entryDenyPermission);
					return false; // Deny entry
				}
			}
		}

		return true; // Allow entry
	}

	/**
	 * Sends a message to a player when entry is denied.
	 * Messages.sendMessageWithCooldown() from the Spigot module.
	 */
	private void sendDeniedMessage(LocalPlayer localPlayer, Player player, ApplicableRegionSet toSet, String flagType, String permission)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}

		final String messageKey;
		if ("entry-permission".equals(flagType))
		{
			messageKey = "entry-permission-denied";
		}
		else // entry-deny-permission
		{
			messageKey = "entry-deny-permission-denied";
		}

		// Send message with cooldown using FoliaLib scheduler (runs on entity thread)
		WorldGuardUtils.getScheduler().runAtEntity(player, task -> {
			if (player.isOnline())
			{
				sendMessageWithCooldown(player, messageKey, new String[]{"permission", permission});
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
			
			if ("entry-permission-denied".equals(key))
			{
				message = org.bukkit.ChatColor.RED + "You don't have permission to enter this area.";
			}
			else if ("entry-deny-permission-denied".equals(key))
			{
				message = org.bukkit.ChatColor.RED + "You are not allowed to enter this area.";
			}
			else
			{
				message = org.bukkit.ChatColor.RED + "Entry denied.";
			}

			if (player.isOnline())
			{
				player.sendMessage(message);
			}
		}
	}
}
