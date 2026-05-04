package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import lombok.RequiredArgsConstructor;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

/**
 * Applies {@code join-location} on {@link PlayerJoinEvent} when the async spawn path did not
 * (e.g. Folia/threading where {@link io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent}
 * handlers cannot reliably query regions). Spawn listener uses {@code null} associable; this uses
 * {@link LocalPlayer} like {@link JoinLocationListener}.
 */
@RequiredArgsConstructor
public final class JoinLocationPlayerJoinListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		org.bukkit.Location at = player.getLocation();
		if (at.getWorld() == null)
		{
			return;
		}

		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		Location target = this.regionContainer.createQuery().queryValue(BukkitAdapter.adapt(at), localPlayer, Flags.JOIN_LOCATION);
		if (target == null)
		{
			return;
		}

		org.bukkit.Location bukkitTarget = BukkitAdapter.adapt(target);
		if (bukkitTarget.getWorld() == null || sameBlock(at, bukkitTarget))
		{
			return;
		}

		WorldGuardUtils.getScheduler().runAtEntity(player, (wrappedTask) -> {
			if (!player.isOnline())
			{
				return;
			}
			player.teleport(bukkitTarget);
		});
	}

	private static boolean sameBlock(org.bukkit.Location a, org.bukkit.Location b)
	{
		if (a.getWorld() == null || !a.getWorld().equals(b.getWorld()))
		{
			return false;
		}
		return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
	}
}
