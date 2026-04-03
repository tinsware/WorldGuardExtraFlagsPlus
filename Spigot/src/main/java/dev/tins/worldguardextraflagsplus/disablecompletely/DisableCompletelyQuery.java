package dev.tins.worldguardextraflagsplus.disablecompletely;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.Messages;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.helpers.BlockableItemFlag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldguard.protection.ApplicableRegionSet;

import java.util.Set;

/**
 * Shared WorldGuard + {@code disable-completely} checks for Bukkit listeners and packet hooks.
 */
public final class DisableCompletelyQuery
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;
	private final Set<String> blockableItems;

	public DisableCompletelyQuery(WorldGuardPlugin worldGuardPlugin, RegionContainer regionContainer,
			SessionManager sessionManager)
	{
		this.worldGuardPlugin = worldGuardPlugin;
		this.regionContainer = regionContainer;
		this.sessionManager = sessionManager;
		this.blockableItems = BlockableItemFlag.getBlockableItems();
	}

	public void sendBlocked(Player player, Material material)
	{
		if (material != null && material != Material.AIR)
		{
			Messages.sendMessageWithCooldown(player, "disable-completely-blocked", "item", material.name());
		}
	}

	public boolean isDisabled(Player player, Material material)
	{
		if (player == null || material == null || material == Material.AIR)
		{
			return false;
		}
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
		{
			return false;
		}
		String name = material.name();
		if (!this.blockableItems.contains(name))
		{
			return false;
		}
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
		Set<String> set = regions.queryValue(localPlayer, Flags.DISABLE_COMPLETELY);
		if (set == null || set.isEmpty())
		{
			return false;
		}
		for (String item : set)
		{
			if (item == null)
			{
				continue;
			}
			if (item.equalsIgnoreCase(name))
			{
				return true;
			}
			if (item.equalsIgnoreCase("SPEAR") && BlockableItemFlag.isSpearMaterial(name))
			{
				return true;
			}
		}
		return false;
	}

	public Material materialFromHand(Player player, boolean mainHand)
	{
		if (player == null)
		{
			return Material.AIR;
		}
		ItemStack stack = mainHand ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
		return stack != null ? stack.getType() : Material.AIR;
	}
}
