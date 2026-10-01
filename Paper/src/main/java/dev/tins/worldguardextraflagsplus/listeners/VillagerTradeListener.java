package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

@RequiredArgsConstructor
public class VillagerTradeListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;
	
	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEntityEvent event)
	{
		if (!(event.getRightClicked() instanceof Villager villager))
		{
			return;
		}
		
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		
		// Check if player has bypass
		if (WorldGuardUtils.hasBypass(this.sessionManager, localPlayer, player))
		{
			return;
		}
		
		// Get region set at villager's location
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(BukkitAdapter.adapt(villager.getLocation()));
		
		// Check villager-trade flag state
		State state = regions.queryState(localPlayer, Flags.VILLAGER_TRADE);
		if (state == State.DENY)
		{
			event.setCancelled(true);
		}
	}
}

