package dev.tins.worldguardextraflagsplus.wg.handlers;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe pearl tracking for the chambered-enderpearl flag.
 * Shared between {@link ChamberedEnderPearlFlagHandler} and {@link ChamberedEnderPearlListener}.
 */
final class ChamberedEnderPearlPearlTracker
{
	private static final ConcurrentHashMap<UUID, Set<Pearl>> TRACKED_PEARLS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<UUID, Boolean> PLAYERS_IN_FLAGGED_REGIONS = new ConcurrentHashMap<>();
	private static final AtomicInteger TRACKED_PEARL_COUNT = new AtomicInteger(0);

	private ChamberedEnderPearlPearlTracker()
	{
	}

	static void setPlayerInFlaggedRegion(UUID playerUUID, boolean inFlaggedRegion)
	{
		boolean wasInFlaggedRegion = PLAYERS_IN_FLAGGED_REGIONS.getOrDefault(playerUUID, false);
		if (inFlaggedRegion == wasInFlaggedRegion)
		{
			return;
		}

		PLAYERS_IN_FLAGGED_REGIONS.put(playerUUID, inFlaggedRegion);

		if (inFlaggedRegion && !wasInFlaggedRegion)
		{
			removeAllTrackedPearlsForPlayer(playerUUID);
		}
	}

	static void onProjectileLaunch(ProjectileLaunchEvent event)
	{
		if (event.getEntityType() != EntityType.ENDER_PEARL)
		{
			return;
		}

		EnderPearl enderPearl = (EnderPearl) event.getEntity();
		if (!(enderPearl.getShooter() instanceof Player player))
		{
			return;
		}

		UUID playerUUID = player.getUniqueId();
		if (PLAYERS_IN_FLAGGED_REGIONS.getOrDefault(playerUUID, false))
		{
			return;
		}

		TRACKED_PEARLS.computeIfAbsent(playerUUID, ignored -> ConcurrentHashMap.newKeySet()).add(new Pearl(enderPearl));
		if (TRACKED_PEARL_COUNT.incrementAndGet() % 10 == 0)
		{
			cleanupInvalidPearls();
		}
	}

	static void onProjectileHit(ProjectileHitEvent event)
	{
		if (!(event.getEntity() instanceof EnderPearl enderPearl))
		{
			return;
		}

		if (!(enderPearl.getShooter() instanceof Player player))
		{
			return;
		}

		removePearlFromTracker(player.getUniqueId(), enderPearl);
	}

	static void onEntityDeath(EntityDeathEvent event)
	{
		if (!(event.getEntity() instanceof EnderPearl enderPearl))
		{
			return;
		}

		if (!(enderPearl.getShooter() instanceof Player player))
		{
			return;
		}

		removePearlFromTracker(player.getUniqueId(), enderPearl);
	}

	static void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
		{
			return;
		}

		Player player = event.getPlayer();
		UUID playerUUID = player.getUniqueId();

		if (!PLAYERS_IN_FLAGGED_REGIONS.getOrDefault(playerUUID, false))
		{
			return;
		}

		Set<Pearl> pearls = TRACKED_PEARLS.get(playerUUID);
		if (pearls == null || pearls.isEmpty())
		{
			return;
		}

		for (Pearl pearl : pearls)
		{
			EnderPearl enderPearl = pearl.pearl.get();
			if (enderPearl != null && enderPearl.isValid() && !enderPearl.isDead())
			{
				event.setCancelled(true);
				removePearlSafely(enderPearl);
				break;
			}
		}
	}

	static void onPlayerQuit(PlayerQuitEvent event)
	{
		UUID playerUUID = event.getPlayer().getUniqueId();
		Set<Pearl> removed = TRACKED_PEARLS.remove(playerUUID);
		if (removed != null)
		{
			TRACKED_PEARL_COUNT.addAndGet(-removed.size());
		}
		PLAYERS_IN_FLAGGED_REGIONS.remove(playerUUID);
	}

	static void cleanupInvalidPearls()
	{
		for (UUID playerUUID : TRACKED_PEARLS.keySet())
		{
			Set<Pearl> pearls = TRACKED_PEARLS.get(playerUUID);
			if (pearls == null)
			{
				continue;
			}

			int before = pearls.size();
			pearls.removeIf(pearl -> {
				EnderPearl enderPearl = pearl.pearl.get();
				return enderPearl == null || !enderPearl.isValid() || enderPearl.isDead();
			});
			int removed = before - pearls.size();
			if (removed > 0)
			{
				TRACKED_PEARL_COUNT.addAndGet(-removed);
			}
			if (pearls.isEmpty())
			{
				TRACKED_PEARLS.remove(playerUUID, pearls);
			}
		}
	}

	private static void removeAllTrackedPearlsForPlayer(UUID playerUUID)
	{
		Set<Pearl> pearls = TRACKED_PEARLS.remove(playerUUID);
		if (pearls == null)
		{
			return;
		}

		for (Pearl pearl : pearls)
		{
			EnderPearl enderPearl = pearl.pearl.get();
			if (enderPearl != null)
			{
				removePearlSafely(enderPearl);
			}
		}
		TRACKED_PEARL_COUNT.addAndGet(-pearls.size());
	}

	private static void removePearlFromTracker(UUID playerUUID, EnderPearl enderPearl)
	{
		Set<Pearl> pearls = TRACKED_PEARLS.get(playerUUID);
		if (pearls == null || pearls.isEmpty())
		{
			return;
		}

		if (pearls.removeIf(pearl -> enderPearl.equals(pearl.pearl.get())))
		{
			TRACKED_PEARL_COUNT.decrementAndGet();
		}
		if (pearls.isEmpty())
		{
			TRACKED_PEARLS.remove(playerUUID, pearls);
		}
	}

	private static void removePearlSafely(EnderPearl enderPearl)
	{
		if (enderPearl == null || enderPearl.isDead())
		{
			return;
		}

		try
		{
			if (!enderPearl.isDead())
			{
				enderPearl.remove();
			}
		}
		catch (Exception e)
		{
			WorldGuardUtils.getScheduler().runAtEntity(enderPearl, task -> {
				if (enderPearl.isValid() && !enderPearl.isDead())
				{
					enderPearl.remove();
				}
			});
		}
	}

	private static final class Pearl
	{
		private final WeakReference<EnderPearl> pearl;

		private Pearl(EnderPearl pearl)
		{
			this.pearl = new WeakReference<>(pearl);
		}
	}
}
