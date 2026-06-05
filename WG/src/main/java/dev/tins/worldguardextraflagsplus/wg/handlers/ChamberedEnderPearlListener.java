package dev.tins.worldguardextraflagsplus.wg.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Bukkit listener for chambered-enderpearl pearl tracking (registered once in onEnable).
 */
public final class ChamberedEnderPearlListener implements Listener
{
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onProjectileLaunch(ProjectileLaunchEvent event)
	{
		ChamberedEnderPearlPearlTracker.onProjectileLaunch(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onProjectileHit(ProjectileHitEvent event)
	{
		ChamberedEnderPearlPearlTracker.onProjectileHit(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event)
	{
		ChamberedEnderPearlPearlTracker.onEntityDeath(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		ChamberedEnderPearlPearlTracker.onPlayerTeleport(event);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		ChamberedEnderPearlPearlTracker.onPlayerQuit(event);
	}
}
