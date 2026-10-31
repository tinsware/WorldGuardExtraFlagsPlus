package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles the {@code destroy-items} flag.
 *
 * <p>When set to {@code false} in a region, dropped items there take no damage,
 * so lava, fire, cactus and explosions can no longer destroy them.</p>
 *
 * <p>Usage: {@code /rg flag <region> destroy-items false}</p>
 */
public class DestroyItemsListener implements Listener
{
    private final RegionContainer regionContainer;

    public DestroyItemsListener(RegionContainer regionContainer)
    {
        this.regionContainer = regionContainer;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event)
    {
        if (event.getEntityType() != EntityType.ITEM)
        {
            return;
        }

        Boolean destroyItems = this.regionContainer.createQuery().queryValue(
                BukkitAdapter.adapt(event.getEntity().getLocation()),
                null,
                Flags.DESTROY_ITEMS);

        if (destroyItems != null && !destroyItems)
        {
            event.setCancelled(true);
        }
    }
}
