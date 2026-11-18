package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.tins.worldguardextraflagsplus.Config;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidTriggerEvent;

/**
 * Handles the {@code raid-enabled} flag (migrated from RegionRaid).
 *
 * <p>Usage: {@code /rg flag <region> raid-enabled deny}</p>
 */
public class RaidListener implements Listener
{
    private final RegionContainer regionContainer;

    public RaidListener(RegionContainer regionContainer)
    {
        this.regionContainer = regionContainer;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent event)
    {
        if (!Config.isFlagEnabled("raid-enabled"))
        {
            return;
        }

        if (isRaidDenied(event.getRaid().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRaidSpawnWave(RaidSpawnWaveEvent event)
    {
        if (!Config.isFlagEnabled("raid-enabled"))
        {
            return;
        }

        if (isRaidDenied(event.getRaid().getLocation()))
        {
            event.getRaiders().forEach(org.bukkit.entity.Entity::remove);
        }
    }


    private boolean isRaidDenied(Location location)
    {
        if (location == null || location.getWorld() == null)
        {
            return false;
        }

        StateFlag.State state = this.regionContainer.createQuery().queryState(
                BukkitAdapter.adapt(location),
                null,
                Flags.RAID_ENABLED);

        return state == StateFlag.State.DENY;
    }
}
