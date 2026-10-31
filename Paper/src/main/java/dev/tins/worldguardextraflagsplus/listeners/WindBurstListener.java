package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the {@code allow-windburst} flag.
 *
 * <p>When set to {@code false} in a region, a hit with a Wind Burst mace still
 * deals its damage, but the enchantment does not launch the attacker. The
 * original hit is cancelled and the same damage is applied again directly,
 * which skips the enchantment's post-attack effect.</p>
 *
 * <p>Usage: {@code /rg flag <region> allow-windburst false}</p>
 */
public class WindBurstListener implements Listener
{
    private final WorldGuardPlugin worldGuardPlugin;
    private final RegionContainer regionContainer;
    private final SessionManager sessionManager;

    // Entities currently receiving the replacement damage, so that hit is not intercepted again
    private final Set<UUID> reapplying = new HashSet<>();

    public WindBurstListener(WorldGuardPlugin worldGuardPlugin,
                             RegionContainer regionContainer,
                             SessionManager sessionManager)
    {
        this.worldGuardPlugin = worldGuardPlugin;
        this.regionContainer = regionContainer;
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWindBurstHit(EntityDamageByEntityEvent event)
    {
        if (!(event.getDamager() instanceof Player damager))
        {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity entity))
        {
            return;
        }

        if (this.reapplying.contains(entity.getUniqueId()))
        {
            return;
        }

        // Cheap item check first, so ordinary hits never reach the region query
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !item.containsEnchantment(Enchantment.WIND_BURST))
        {
            return;
        }

        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(damager);
        if (WorldGuardUtils.hasBypass(this.sessionManager, localPlayer, damager))
        {
            return;
        }

        Boolean allowWindBurst = this.regionContainer.createQuery().queryValue(
                BukkitAdapter.adapt(damager.getLocation()),
                localPlayer,
                Flags.ALLOW_WIND_BURST);

        if (allowWindBurst == null || allowWindBurst)
        {
            return;
        }

        event.setCancelled(true);

        UUID uuid = entity.getUniqueId();
        this.reapplying.add(uuid);
        try
        {
            entity.damage(event.getDamage(), damager);
        }
        finally
        {
            this.reapplying.remove(uuid);
        }
    }
}
