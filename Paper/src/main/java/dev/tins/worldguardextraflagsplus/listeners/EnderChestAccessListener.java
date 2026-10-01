package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.Messages;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles the {@code enderchest-access} flag.
 *
 * <p>When set to {@code deny} in a region, players cannot open ender chests at that
 * block location. Semantics match WorldGuard {@code chest-access}, but for ender chests only.</p>
 *
 * <p>Usage: {@code /rg flag <region> enderchest-access deny}</p>
 */
public class EnderChestAccessListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;

	public EnderChestAccessListener(WorldGuardPlugin worldGuardPlugin,
			RegionContainer regionContainer,
			SessionManager sessionManager)
	{
		this.worldGuardPlugin = worldGuardPlugin;
		this.regionContainer = regionContainer;
		this.sessionManager = sessionManager;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event)
	{
		if (event.getInventory().getType() != InventoryType.ENDER_CHEST)
		{
			return;
		}

		if (!(event.getPlayer() instanceof Player player))
		{
			return;
		}

		Block block = event.getInventory().getLocation() != null
				? event.getInventory().getLocation().getBlock()
				: null;
		if (block == null || block.getType() != Material.ENDER_CHEST)
		{
			return;
		}

		if (denyAccess(player, block))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event)
	{
		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.ENDER_CHEST)
		{
			return;
		}

		if (denyAccess(event.getPlayer(), block))
		{
			event.setCancelled(true);
		}
	}

	private boolean denyAccess(Player player, Block block)
	{
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);

		if (WorldGuardUtils.hasBypass(this.sessionManager, localPlayer, player))
		{
			return false;
		}

		ApplicableRegionSet regions = this.regionContainer.createQuery()
				.getApplicableRegions(BukkitAdapter.adapt(block.getLocation()));
		State state = regions.queryState(localPlayer, Flags.ENDERCHEST_ACCESS);
		if (state != State.DENY)
		{
			return false;
		}

		Messages.sendMessageWithCooldown(player, "enderchest-access-denied");
		return true;
	}
}
