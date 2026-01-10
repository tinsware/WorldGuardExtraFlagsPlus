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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class PlayerCountLimitFlagHandler extends Handler
{
	public static final Factory FACTORY(Plugin plugin)
	{
		return new Factory(plugin);
	}

	public static class Factory extends Handler.Factory<PlayerCountLimitFlagHandler>
	{
		private final Plugin plugin;

		public Factory(Plugin plugin)
		{
			this.plugin = plugin;
		}

		@Override
		public PlayerCountLimitFlagHandler create(Session session)
		{
			return new PlayerCountLimitFlagHandler(this.plugin, session);
		}
	}

	private final Plugin plugin;

	public PlayerCountLimitFlagHandler(Plugin plugin, Session session)
	{
		super(session);
		this.plugin = plugin;
	}

	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
	                              Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		// Check if player has bypass
		if (this.getSession().getManager().hasBypass(player, (World) to.getExtent()))
		{
			return true; // Allow entry if player has bypass
		}

		// Check player count limits for entered regions
		for (ProtectedRegion region : entered)
		{
			String limitStr = region.getFlag(Flags.PLAYER_COUNT_LIMIT);
			if (limitStr != null && !limitStr.isEmpty())
			{
				try
				{
					int limit = Integer.parseInt(limitStr.trim());
					if (limit > 0)
					{
						int currentPlayers = countPlayersInRegion(region);
						if (currentPlayers >= limit)
						{
							// Region is full, deny entry
							sendRegionFullMessage(((BukkitPlayer) player).getPlayer(), limit);
							return false;
						}
					}
				}
				catch (NumberFormatException e)
				{
					// Invalid number format, skip this region
					continue;
				}
			}
		}

		return true; // Allow entry
	}

	/**
	 * Counts the number of players currently in the specified region.
	 *
	 * @param region The region to count players in
	 * @return The number of players in the region
	 */
	private int countPlayersInRegion(ProtectedRegion region)
	{
		int count = 0;

		for (Player onlinePlayer : Bukkit.getOnlinePlayers())
		{
			// Check if player is in the region
			com.sk89q.worldedit.math.BlockVector3 blockVector = com.sk89q.worldedit.math.BlockVector3.at(
				onlinePlayer.getLocation().getBlockX(),
				onlinePlayer.getLocation().getBlockY(),
				onlinePlayer.getLocation().getBlockZ()
			);
			if (region.contains(blockVector))
			{
				count++;
			}
		}

		return count;
	}

	/**
	 * Sends a message to the player indicating the region is full.
	 *
	 * @param player The player to send the message to
	 * @param limit The maximum number of players allowed
	 */
	private void sendRegionFullMessage(Player player, int limit)
	{
		// Use reflection to call Messages.sendMessageWithCooldown() from Spigot module
		try
		{
			Class<?> messagesClass = Class.forName("dev.tins.worldguardextraflagsplus.Messages");
			java.lang.reflect.Method sendMessageMethod = messagesClass.getMethod("sendMessageWithCooldown",
				Player.class, String.class, String[].class);

			// Send message with limit placeholder
			sendMessageMethod.invoke(null, player, "player-count-limit-denied",
				new String[]{"limit", String.valueOf(limit)});
		}
		catch (Exception e)
		{
			// Fallback: send basic message if reflection fails
			if (player.isOnline())
			{
				player.sendMessage("§cThis region is full! Maximum players: " + limit);
			}
		}
	}
}
