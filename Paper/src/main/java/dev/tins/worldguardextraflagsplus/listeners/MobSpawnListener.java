package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.helpers.MobSpawnDenyCategoryFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Set;

/**
 * Handles the {@code deny-mobspawn} flag.
 *
 * <p>When set on a region, spawning of mobs in the listed categories is cancelled.
 * The check is a non-player region query at the spawn location, so it works for
 * spawners, spawn eggs, natural spawns and every other {@link CreatureSpawnEvent}
 * source. See {@link MobSpawnDenyCategoryFlag} for the category definitions.</p>
 *
 * <p>Usage: {@code /rg flag <region> deny-mobspawn ALL_HOSTILE}</p>
 */
public class MobSpawnListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;

	public MobSpawnListener(WorldGuardPlugin worldGuardPlugin, RegionContainer regionContainer)
	{
		this.worldGuardPlugin = worldGuardPlugin;
		this.regionContainer = regionContainer;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event)
	{
		Set<String> values = this.regionContainer.createQuery().queryValue(
				BukkitAdapter.adapt(event.getLocation()),
				null,
				Flags.DENY_MOB_SPAWN);

		if (values != null && !values.isEmpty()
				&& MobSpawnDenyCategoryFlag.isBlockedBy(event.getEntityType(), values))
		{
			event.setCancelled(true);
		}
	}
}
