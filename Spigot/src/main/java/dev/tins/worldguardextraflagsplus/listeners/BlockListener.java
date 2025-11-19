package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;

import com.sk89q.worldguard.protection.flags.StateFlag.State;

import lombok.RequiredArgsConstructor;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.Config;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

@RequiredArgsConstructor
public class BlockListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;
	
	// Workbench type mappings (same as EntityListener)
	private static final Map<Material, String> WORKBENCH_TYPE_MAP = new HashMap<>();
	static
	{
		WORKBENCH_TYPE_MAP.put(Material.ANVIL, "ANVIL");
		WORKBENCH_TYPE_MAP.put(Material.CHIPPED_ANVIL, "ANVIL");
		WORKBENCH_TYPE_MAP.put(Material.DAMAGED_ANVIL, "ANVIL");
		WORKBENCH_TYPE_MAP.put(Material.CARTOGRAPHY_TABLE, "CARTOGRAPHY");
		WORKBENCH_TYPE_MAP.put(Material.CRAFTING_TABLE, "CRAFT");
		WORKBENCH_TYPE_MAP.put(Material.ENDER_CHEST, "ENDER");
		WORKBENCH_TYPE_MAP.put(Material.GRINDSTONE, "GRINDSTONE");
		WORKBENCH_TYPE_MAP.put(Material.LOOM, "LOOM");
		WORKBENCH_TYPE_MAP.put(Material.SMITHING_TABLE, "SMITHING");
		WORKBENCH_TYPE_MAP.put(Material.STONECUTTER, "STONECUTTER");
	}
	
	/**
	 * Checks if a workbench block placement should be blocked
	 */
	private boolean isWorkbenchPlacementBlocked(LocalPlayer localPlayer, Material blockMaterial, Location blockLocation)
	{
		// Check if config allows blocking placement
		if (!Config.isPermitWorkbenchBlockPlacementToo())
		{
			return false;
		}
		
		// Check if this is a workbench block
		String workbenchType = WORKBENCH_TYPE_MAP.get(blockMaterial);
		if (workbenchType == null)
		{
			return false;
		}
		
		// Check if flag is set in region (check player location for performance)
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
		Set<String> flagSet = regions.queryValue(localPlayer, Flags.PERMIT_WORKBENCHES);
		if (flagSet == null || flagSet.isEmpty())
		{
			return false;
		}
		
		// Check for "CLEAR" keyword
		for (String value : flagSet)
		{
			if (value != null && value.equalsIgnoreCase("CLEAR"))
			{
				return false;
			}
		}
		
		// Check for specific workbench type FIRST (explicit values always take precedence)
		// This ensures "ender" in "all,ender" always blocks ender chests regardless of config
		for (String value : flagSet)
		{
			if (value != null && value.equalsIgnoreCase(workbenchType))
			{
				return true;
			}
		}
		
		// Check for "ALL" keyword (only if specific type not found)
		boolean hasAll = false;
		boolean allIncludesEnder = Config.isPermitAllIncludesEnderchest();
		for (String value : flagSet)
		{
			if (value != null && value.equalsIgnoreCase("ALL"))
			{
				hasAll = true;
				break;
			}
		}
		
		if (hasAll)
		{
			// If "ALL" is set, block all workbenches except ender chest (unless config says otherwise)
			if (workbenchType.equals("ENDER"))
			{
				return allIncludesEnder;
			}
			return true;
		}
		
		return false;
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onBlockPlaceEvent(PlaceBlockEvent event)
	{
		Event.Result originalResult = event.getResult();
		Object cause = event.getCause().getRootCause();
		
		if (!(cause instanceof Player player))
		{
			return;
		}
		
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
		{
			return;
		}
		
		for (Block block : event.getBlocks())
		{
			Material type = block.getType();
			if (type == Material.AIR)
			{
				type = event.getEffectiveMaterial();
			}
			
			Location location = BukkitAdapter.adapt(block.getLocation());
			ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(location);
			
			// Check allow-block-place first
			Set<Material> allowSet = regions.queryValue(localPlayer, Flags.ALLOW_BLOCK_PLACE);
			if (allowSet != null && !allowSet.isEmpty() && allowSet.contains(type))
			{
				event.setResult(Event.Result.ALLOW);
				continue;
			}
			
			// Check deny-block-place
			Set<Material> denySet = regions.queryValue(localPlayer, Flags.DENY_BLOCK_PLACE);
			if (denySet != null && !denySet.isEmpty() && denySet.contains(type))
			{
				event.setResult(Event.Result.DENY);
				return;
			}
			
			// Check permit-workbenches for block placement (if config enabled)
			if (isWorkbenchPlacementBlocked(localPlayer, type, location))
			{
				event.setResult(Event.Result.DENY);
				return;
			}
			
			// Restore original result if no flags matched
			event.setResult(originalResult);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onBlockBreakEvent(BreakBlockEvent event)
	{
		Event.Result originalResult = event.getResult();
		Object cause = event.getCause().getRootCause();
		
		if (!(cause instanceof Player player))
		{
			return;
		}
		
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
		{
			return;
		}
		
		for (Block block : event.getBlocks())
		{
			Material type = block.getType();
			Location location = BukkitAdapter.adapt(block.getLocation());
			ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(location);
			
			// Check allow-block-break first
			Set<Material> allowSet = regions.queryValue(localPlayer, Flags.ALLOW_BLOCK_BREAK);
			if (allowSet != null && !allowSet.isEmpty() && allowSet.contains(type))
			{
				event.setResult(Event.Result.ALLOW);
				continue;
			}
			
			// Check deny-block-break
			Set<Material> denySet = regions.queryValue(localPlayer, Flags.DENY_BLOCK_BREAK);
			if (denySet != null && !denySet.isEmpty() && denySet.contains(type))
			{
				event.setResult(Event.Result.DENY);
				return;
			}
			
			// Restore original result if no flags matched
			event.setResult(originalResult);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityBlockFormEvent(EntityBlockFormEvent event)
	{
		BlockState newState = event.getNewState();
		if (newState.getType() == Material.FROSTED_ICE)
		{
			Location location = BukkitAdapter.adapt(newState.getLocation());

			LocalPlayer localPlayer;
			if (event.getEntity() instanceof Player player)
			{
				localPlayer = this.worldGuardPlugin.wrapPlayer(player);
				if (this.sessionManager.hasBypass(localPlayer, (World) location.getExtent()))
				{
					return;
				}
			}
			else
			{
				localPlayer = null;
			}

			if (this.regionContainer.createQuery().queryValue(location, localPlayer, Flags.FROSTWALKER) == State.DENY)
			{
				event.setCancelled(true);
			}
		}
	}
}



