package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.flags.helpers.ForcedStateFlag;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.world.PortalCreateEvent;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;

import lombok.RequiredArgsConstructor;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.helpers.BlockableItemFlag;
import dev.tins.worldguardextraflagsplus.Messages;

import java.util.Set;

@RequiredArgsConstructor
public class EntityListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;

	// Get blockable items list from the flag helper (single source of truth)
	private static final Set<String> BLOCKABLE_ITEMS = BlockableItemFlag.getBlockableItems();

	@EventHandler(ignoreCancelled = true)
	public void onPortalCreateEvent(PortalCreateEvent event)
	{
		LocalPlayer localPlayer;
		if (event.getEntity() instanceof Player player)
		{
			localPlayer = this.worldGuardPlugin.wrapPlayer(player);
			if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
			{
				return;
			}
		}
		else
		{
			localPlayer = null;
		}

		for (BlockState block : event.getBlocks())
		{
			if (this.regionContainer.createQuery().queryState(BukkitAdapter.adapt(block.getLocation()), localPlayer, Flags.NETHER_PORTALS) == State.DENY)
			{
				event.setCancelled(true);
				break;
			}
		}
	}

    private boolean isBlocked(LocalPlayer localPlayer, Material material)
    {
        String name = material.name();
        
        // Early exit: only check flag if item is in our hardcoded blockable list
        if (!BLOCKABLE_ITEMS.contains(name))
        {
            return false;
        }
        
        // Check if flag is set in region (inheritance handled automatically by WorldGuard)
        ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
        
        // Check new flag first (disable-completely)
        java.util.Set<String> set = regions.queryValue(localPlayer, Flags.DISABLE_COMPLETELY);
        boolean usingDeprecatedFlag = false;
        
        // Fall back to deprecated flag if new flag is not set
        if (set == null || set.isEmpty())
        {
            set = regions.queryValue(localPlayer, Flags.PERMIT_COMPLETELY);
            usingDeprecatedFlag = (set != null && !set.isEmpty());
            
            // Log deprecation warning if old flag is used
            if (usingDeprecatedFlag)
            {
                // Get region name for warning (use first applicable region)
                for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regions)
                {
                    this.worldGuardPlugin.getLogger().warning(
                        "Region '" + region.getId() + "' uses deprecated flag 'permit-completely'. " +
                        "Please update to 'disable-completely'. " +
                        "Run: /rg flag " + region.getId() + " disable-completely <items>"
                    );
                    break; // Only log once per check
                }
            }
        }
        
        if (set == null || set.isEmpty())
        {
            return false;
        }
        
        // Case-insensitive check against flag set
        for (String item : set)
        {
            if (item != null && item.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    private void sendBlocked(Player player, String itemName)
    {
        // Use cooldown-aware message sending (try new key first, fall back to old for compatibility)
        if (!Messages.sendMessageWithCooldown(player, "disable-completely-blocked", "item", itemName))
        {
            // Fall back to old message key if new one doesn't exist
            Messages.sendMessageWithCooldown(player, "permit-completely-blocked", "item", itemName);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onItemInteract(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();
        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
        if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
        {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) return;
        Material mat = item.getType();
        if (mat == Material.AIR) return;
        
        // Check if item is blocked
        if (this.isBlocked(localPlayer, mat))
        {
            // Special handling for tridents with Riptide - prevent the hold interaction
            if (mat == Material.TRIDENT && item.containsEnchantment(org.bukkit.enchantments.Enchantment.RIPTIDE))
            {
                // Check if it's any right-click action (when player starts holding)
                // Block all right-click types to ensure Riptide cannot be activated
                org.bukkit.event.block.Action action = event.getAction();
                if (action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR 
                    || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                {
                    event.setCancelled(true);
                    this.sendBlocked(player, mat.name());
                    return;
                }
            }
            
            // For other items or non-riptide interactions, cancel normally
            event.setCancelled(true);
            this.sendBlocked(player, mat.name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event)
    {
        Player player = event.getPlayer();
        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
        if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
        {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        Material mat = item != null ? item.getType() : Material.AIR;
        if (mat != Material.AIR && this.isBlocked(localPlayer, mat))
        {
            event.setCancelled(true);
            this.sendBlocked(player, mat.name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event)
    {
        if (!(event.getDamager() instanceof Player player))
        {
            return;
        }
        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
        if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
        {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        Material mat = item != null ? item.getType() : Material.AIR;
        if (mat != Material.AIR && this.isBlocked(localPlayer, mat))
        {
            event.setCancelled(true);
            this.sendBlocked(player, mat.name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event)
    {
        if (!(event.getEntity().getShooter() instanceof Player player))
        {
            return;
        }
        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
        if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
        {
            return;
        }
        // Try to infer from main hand item
        ItemStack item = player.getInventory().getItemInMainHand();
        Material mat = item != null ? item.getType() : Material.AIR;
        if (mat != Material.AIR && this.isBlocked(localPlayer, mat))
        {
            event.setCancelled(true);
            this.sendBlocked(player, mat.name());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerRiptide(PlayerRiptideEvent event)
    {
        // Skip if already cancelled by another plugin
        if (event instanceof Cancellable cancellable && cancellable.isCancelled())
        {
            return;
        }
        
        Player player = event.getPlayer();
        LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
        if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
        {
            return;
        }
        
        // Get the trident item from the event
        ItemStack item = event.getItem();
        if (item == null) return;
        Material mat = item.getType();
        
        // Check if TRIDENT is blocked
        if (mat == Material.TRIDENT && this.isBlocked(localPlayer, mat))
        {
            // Cancel the event to prevent riptide boost
            if (event instanceof Cancellable cancellable)
            {
                cancellable.setCancelled(true);
            }
            this.sendBlocked(player, mat.name());
            

            // NOTE: Velocity cancellation code commented out to avoid unnecessary tasks
            // Since interaction blocking prevents Riptide from starting, velocity cancellation
            // is not needed. If issues arise, uncomment the code below.
            /*

            // Store current rotation to prevent spinning animation
            float originalYaw = player.getLocation().getYaw();
            float originalPitch = player.getLocation().getPitch();
            

            // Store current velocity before riptide boost is applied
            org.bukkit.util.Vector originalVelocity = player.getVelocity().clone();
            
            // Prevent the riptide boost by canceling velocity and locking rotation
            // Use multiple ticks to ensure we catch any velocity changes
            WorldGuardUtils.getScheduler().runAtEntity(player, task -> {
                if (!player.isOnline()) return;
                
                // Lock rotation to prevent spinning animation
                org.bukkit.Location loc = player.getLocation();
                loc.setYaw(originalYaw);
                loc.setPitch(originalPitch);
                player.teleport(loc);
                
                // Reset velocity to prevent the riptide boost
                org.bukkit.util.Vector velocity = player.getVelocity();
                
                // Check if velocity significantly increased (riptide boost applied)
                double velocityMagnitude = velocity.length();
                double originalMagnitude = originalVelocity.length();
                
                // If velocity increased significantly, cancel the boost
                if (velocityMagnitude > originalMagnitude + 0.5)
                {
                    // Cancel horizontal momentum from riptide boost
                    velocity.setX(0);
                    velocity.setZ(0);
                    // Reduce vertical velocity if player was launched upward
                    if (velocity.getY() > 0.3)
                    {
                        velocity.setY(Math.min(velocity.getY() * 0.2, 0.1));
                    }
                    else
                    {
                        // Keep original vertical velocity if not boosted
                        velocity.setY(originalVelocity.getY());
                    }
                    player.setVelocity(velocity);
                }
                
                // Schedule additional checks for delayed velocity application and rotation lock
                // Check again after 1 tick
                WorldGuardUtils.getScheduler().runAtEntity(player, task2 -> {
                    if (!player.isOnline()) return;
                    
                    // Continue locking rotation
                    org.bukkit.Location loc2 = player.getLocation();
                    loc2.setYaw(originalYaw);
                    loc2.setPitch(originalPitch);
                    player.teleport(loc2);
                    
                    org.bukkit.util.Vector vel2 = player.getVelocity();
                    if (vel2.length() > 0.5)
                    {
                        vel2.setX(0);
                        vel2.setZ(0);
                        if (vel2.getY() > 0.3)
                        {
                            vel2.setY(Math.min(vel2.getY() * 0.2, 0.1));
                        }
                        player.setVelocity(vel2);
                    }
                    
                    // Final check after 2 more ticks
                    WorldGuardUtils.getScheduler().runAtEntity(player, task3 -> {
                        if (!player.isOnline()) return;
                        
                        // Final rotation lock
                        org.bukkit.Location loc3 = player.getLocation();
                        loc3.setYaw(originalYaw);
                        loc3.setPitch(originalPitch);
                        player.teleport(loc3);
                        
                        org.bukkit.util.Vector vel3 = player.getVelocity();
                        if (vel3.length() > 0.5)
                        {
                            vel3.setX(0);
                            vel3.setZ(0);
                            if (vel3.getY() > 0.3)
                            {
                                vel3.setY(Math.min(vel3.getY() * 0.2, 0.1));
                            }
                            player.setVelocity(vel3);
                        }
                    });
                });
            });

            
            // Lock rotation to prevent spinning animation (simplified version without velocity cancellation)
            WorldGuardUtils.getScheduler().runAtEntity(player, task -> {
                if (!player.isOnline()) return;
                
                // Lock rotation to prevent spinning animation
                org.bukkit.Location loc = player.getLocation();
                loc.setYaw(originalYaw);
                loc.setPitch(originalPitch);
                player.teleport(loc);
                
                // Additional rotation lock after 1 tick
                WorldGuardUtils.getScheduler().runAtEntity(player, task2 -> {
                    if (!player.isOnline()) return;
                    
                    org.bukkit.Location loc2 = player.getLocation();
                    loc2.setYaw(originalYaw);
                    loc2.setPitch(originalPitch);
                    player.teleport(loc2);
                    
                    // Final rotation lock after 2 more ticks
                    WorldGuardUtils.getScheduler().runAtEntity(player, task3 -> {
                        if (!player.isOnline()) return;
                        
                        org.bukkit.Location loc3 = player.getLocation();
                        loc3.setYaw(originalYaw);
                        loc3.setPitch(originalPitch);
                        player.teleport(loc3);
                    });
                });
            });
            */


        }
    }

	@EventHandler(ignoreCancelled = true)
	public void onEntityResurrectEvent(EntityResurrectEvent event)
	{
		Entity entity = event.getEntity();
		if (!(entity instanceof Player player))
		{
			return;
		}

		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
		{
			return;
		}

		// Check if player has totem in main hand or off hand
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		
		Material totemMaterial = Material.TOTEM_OF_UNDYING;
		
		// Check main hand totem
		if (mainHand != null && mainHand.getType() == totemMaterial && this.isBlocked(localPlayer, totemMaterial))
		{
			event.setCancelled(true);
			this.sendBlocked(player, totemMaterial.name());
			return;
		}
		
		// Check off hand totem
		if (offHand != null && offHand.getType() == totemMaterial && this.isBlocked(localPlayer, totemMaterial))
		{
			event.setCancelled(true);
			this.sendBlocked(player, totemMaterial.name());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityToggleGlideEvent(EntityToggleGlideEvent event)
	{
		Entity entity = event.getEntity();
		if (entity instanceof Player player)
		{
			LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
			if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
			{
				return;
			}

			ForcedStateFlag.ForcedState state = this.regionContainer.createQuery().queryValue(localPlayer.getLocation(), localPlayer, Flags.GLIDE);
			switch(state)
			{
				case ALLOW:
					break;
				case DENY:
				{
					if (!event.isGliding())
					{
						return;
					}

					event.setCancelled(true);

					//Prevent the player from being allowed to glide by spamming space
					// Push player down slightly to cancel upward momentum
					WorldGuardUtils.getScheduler().runAtEntity(player, task -> {
						org.bukkit.util.Vector velocity = player.getVelocity();
						velocity.setY(Math.min(velocity.getY(), -0.5));
						player.setVelocity(velocity);
					});

					break;
				}
				case FORCE:
				{
					if (event.isGliding())
					{
						return;
					}

					event.setCancelled(true);

					break;
				}
			}
		}
	}
}



