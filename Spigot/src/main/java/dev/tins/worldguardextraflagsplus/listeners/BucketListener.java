package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.listeners.BucketAllowSupport.AllowDecision;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import dev.tins.worldguardextraflagsplus.listeners.BucketAllowSupport.BucketMaterials;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

@RequiredArgsConstructor
public class BucketListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPlayerBucketFill(PlayerBucketFillEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (WorldGuardUtils.hasBypass(this.sessionManager, localPlayer, player))
		{
			return;
		}

		Block block = event.getBlock();
		BucketMaterials materials = BucketAllowSupport.resolveBucketMaterials(block);
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(BukkitAdapter.adapt(block.getLocation()));

		applyDecision(event, BucketAllowSupport.evaluateBreakAllow(localPlayer, regions, materials));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (WorldGuardUtils.hasBypass(this.sessionManager, localPlayer, player))
		{
			return;
		}

		Block block = event.getBlock();
		Material liquidMaterial = event.getBucket() == Material.LAVA_BUCKET ? Material.LAVA : Material.WATER;
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(BukkitAdapter.adapt(block.getLocation()));

		applyDecision(event, BucketAllowSupport.evaluatePlaceAllow(localPlayer, regions, liquidMaterial));
	}

	private static void applyDecision(org.bukkit.event.player.PlayerBucketEvent event, AllowDecision decision)
	{
		switch (decision)
		{
			case ALLOW:
				event.setCancelled(false);
				break;
			case DENY:
				event.setCancelled(true);
				break;
			case NO_MATCH:
			default:
				break;
		}
	}
}
