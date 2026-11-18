package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.tins.worldguardextraflagsplus.Config;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.helpers.MobSpawnDenyCategoryFlag;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles {@code mob-damage-deny} and {@code mob-damage-allow} flags.
 *
 * <p>Supports region groups via {@code -g} (e.g. {@code -g NON_MEMBER}, {@code -g NON_OWNER},
 * or {@code ALL} by default — blocking even region owners when set without {@code -g}).</p>
 */
public class MobDamageListener implements Listener
{
    private static final long MESSAGE_COOLDOWN_MS = 1500L;

    private final WorldGuardPlugin worldGuardPlugin;
    private final RegionContainer regionContainer;
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();

    public MobDamageListener(WorldGuardPlugin worldGuardPlugin, RegionContainer regionContainer)
    {
        this.worldGuardPlugin = worldGuardPlugin;
        this.regionContainer = regionContainer;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity) || target instanceof Player || target.hasMetadata("NPC"))
        {
            return;
        }

        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null)
        {
            return;
        }

        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(attacker);
        Location loc = BukkitAdapter.adapt(target.getLocation());
        RegionQuery query = this.regionContainer.createQuery();

        Set<String> denyValues = Config.isFlagEnabled("mob-damage-deny")
                ? query.queryValue(loc, localPlayer, Flags.MOB_DAMAGE_DENY)
                : null;
        Set<String> allowValues = Config.isFlagEnabled("mob-damage-allow")
                ? query.queryValue(loc, localPlayer, Flags.MOB_DAMAGE_ALLOW)
                : null;

        EntityType targetType = target.getType();
        boolean isDenied = denyValues != null && !denyValues.isEmpty()
                && MobSpawnDenyCategoryFlag.isBlockedBy(targetType, denyValues);
        boolean isAllowed = allowValues != null && !allowValues.isEmpty()
                && MobSpawnDenyCategoryFlag.isBlockedBy(targetType, allowValues);

        if (!isDenied && !isAllowed)
        {
            return;
        }

        if (Config.isDenyFirst())
        {
            if (isDenied)
            {
                event.setCancelled(true);
                sendDenyMessage(attacker, localPlayer, query, loc);
            }
            else if (isAllowed)
            {
                event.setCancelled(false);
            }
        }
        else
        {
            if (isAllowed)
            {
                event.setCancelled(false);
            }
            else if (isDenied)
            {
                event.setCancelled(true);
                sendDenyMessage(attacker, localPlayer, query, loc);
            }
        }
    }

    private void sendDenyMessage(Player player, LocalPlayer localPlayer, RegionQuery query, Location loc)
    {
        long now = System.currentTimeMillis();
        Long lastSent = this.messageCooldowns.get(player.getUniqueId());
        if (lastSent != null && (now - lastSent) < MESSAGE_COOLDOWN_MS)
        {
            return;
        }
        this.messageCooldowns.put(player.getUniqueId(), now);

        String customRegionMsg = query.queryValue(loc, localPlayer, com.sk89q.worldguard.protection.flags.Flags.DENY_MESSAGE);
        if (customRegionMsg != null && !customRegionMsg.isEmpty())
        {
            String what = player.locale().getLanguage().equalsIgnoreCase("ru")
                    ? "причинять вред этому"
                    : "harm that";
            localPlayer.printRaw(customRegionMsg.replace("%what%", what));
            return;
        }

        String fallbackMsg = Config.getMobDamageDenyMessage();
        if (fallbackMsg != null && !fallbackMsg.isEmpty())
        {
            localPlayer.printRaw(fallbackMsg);
        }
    }


    private Player resolvePlayerAttacker(Entity damager)
    {
        if (damager instanceof Player player)
        {
            return player;
        }
        if (damager instanceof Projectile projectile)
        {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player)
            {
                return player;
            }
        }
        if (damager instanceof AreaEffectCloud cloud)
        {
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player player)
            {
                return player;
            }
        }
        if (damager instanceof TNTPrimed tnt)
        {
            Entity source = tnt.getSource();
            if (source instanceof Player player)
            {
                return player;
            }
        }
        return null;
    }
}
