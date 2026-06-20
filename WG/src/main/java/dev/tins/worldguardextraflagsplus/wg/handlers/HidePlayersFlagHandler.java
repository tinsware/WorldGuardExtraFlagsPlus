package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

import com.sk89q.worldguard.session.Session;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hides players from others while inside a region with {@code hide-players: true}.
 * Uses Bukkit's {@link Player#hidePlayer(Plugin, Player)} API (experimental hub/lobby optimization).
 */
public class HidePlayersFlagHandler extends FlagValueChangeHandler<Boolean>
{
	private static final Set<UUID> HIDDEN_PLAYERS = ConcurrentHashMap.newKeySet();

	public static final Factory FACTORY = new Factory();

	public static class Factory extends Handler.Factory<HidePlayersFlagHandler>
	{
		@Override
		public HidePlayersFlagHandler create(Session session)
		{
			return new HidePlayersFlagHandler(session);
		}
	}

	public static boolean isPlayerHidden(UUID playerId)
	{
		return playerId != null && HIDDEN_PLAYERS.contains(playerId);
	}

	public static void clearPlayerCaches(UUID playerId)
	{
		if (playerId == null)
		{
			return;
		}

		HIDDEN_PLAYERS.remove(playerId);
	}

	public static void applyVisibilityForJoiningPlayer(Player joiningPlayer)
	{
		Plugin plugin = WorldGuardUtils.getPlugin();
		if (plugin == null || joiningPlayer == null)
		{
			return;
		}

		for (Player online : Bukkit.getOnlinePlayers())
		{
			if (online.equals(joiningPlayer))
			{
				continue;
			}

			if (HIDDEN_PLAYERS.contains(online.getUniqueId()))
			{
				joiningPlayer.hidePlayer(plugin, online);
			}
		}
	}

	protected HidePlayersFlagHandler(Session session)
	{
		super(session, Flags.HIDE_PLAYERS);
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Boolean value)
	{
		this.handleValue(player, (World) player.getWorld(), value);
	}

	@Override
	protected boolean onSetValue(
			LocalPlayer player,
			Location from,
			Location to,
			ApplicableRegionSet toSet,
			Boolean currentValue,
			Boolean lastValue,
			MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(
			LocalPlayer player,
			Location from,
			Location to,
			ApplicableRegionSet toSet,
			Boolean lastValue,
			MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}

	private void handleValue(LocalPlayer player, World world, Boolean value)
	{
		if (!(player instanceof BukkitPlayer bukkitPlayer))
		{
			return;
		}

		Player bukkit = bukkitPlayer.getPlayer();
		if (bukkit == null || !WorldGuardUtils.isPluginEnabled())
		{
			return;
		}

		if (this.getSession().getManager().hasBypass(player, world))
		{
			WorldGuardUtils.getScheduler().runAtEntity(bukkit, (wrappedTask) ->
					this.applyVisibility(bukkit, false));
			return;
		}

		boolean hide = Boolean.TRUE.equals(value);
		WorldGuardUtils.getScheduler().runAtEntity(bukkit, (wrappedTask) ->
				this.applyVisibility(bukkit, hide));
	}

	private void applyVisibility(Player subject, boolean hide)
	{
		Plugin plugin = WorldGuardUtils.getPlugin();
		if (plugin == null || !subject.isOnline())
		{
			return;
		}

		UUID subjectId = subject.getUniqueId();
		if (hide)
		{
			HIDDEN_PLAYERS.add(subjectId);
		}
		else
		{
			HIDDEN_PLAYERS.remove(subjectId);
		}

		for (Player viewer : Bukkit.getOnlinePlayers())
		{
			if (viewer.equals(subject))
			{
				continue;
			}

			if (hide)
			{
				if (viewer.canSee(subject))
				{
					viewer.hidePlayer(plugin, subject);
				}
			}
			else if (!viewer.canSee(subject))
			{
				viewer.showPlayer(plugin, subject);
			}
		}
	}
}
