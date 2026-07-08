package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Handles the {@code lightning-damage} flag.
 *
 * <p>When set to {@code deny} in a region, lightning strikes will still appear
 * visually (the strike effect plays) but players inside the region will not
 * take any damage from lightning. Useful for PvP arenas that want the visual
 * effect without the environmental hazard.</p>
 *
 * <p>Usage: {@code /rg flag <region> lightning-damage deny}</p>
 */
public class LightningDamageListener implements Listener
{
    private final WorldGuardPlugin worldGuardPlugin;
    private final RegionContainer regionContainer;
    private final SessionManager sessionManager;

    public LightningDamageListener(WorldGuardPlugin worldGuardPlugin,
                                   RegionContainer regionContainer,
                                   SessionManager sessionManager)
    {
        this.worldGuardPlugin = worldGuardPlugin;
        this.regionContainer = regionContainer;
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLightningDamage(EntityDamageEvent event)
    {
        // Only intercept lightning damage
        if (event.getCause() != DamageCause.LIGHTNING)
        {
            return;
        }

        // Only protect players
        if (!(event.getEntity() instanceof Player player))
        {
            return;
        }

        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);

        // Bypass for players with WorldGuard bypass permission
        if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
        {
            return;
        }

        // Check lightning-damage flag at the player's location
        State state = this.regionContainer.createQuery().queryState(
                BukkitAdapter.adapt(player.getLocation()),
                localPlayer,
                Flags.LIGHTNING_DAMAGE);

        // Cancel damage if the flag is explicitly denied
        if (state == State.DENY)
        {
            event.setCancelled(true);
        }
    }
}
