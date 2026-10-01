package dev.tins.worldguardextraflagsplus.disablethrow;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.Messages;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.helpers.ThrowableItemFlag;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.Material;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.inventory.PlayerInventory;

import com.sk89q.worldguard.protection.ApplicableRegionSet;

import java.util.Set;

/**
 * WorldGuard {@code disable-throw} checks — mirrors {@link dev.tins.worldguardextraflagsplus.disablecompletely.DisableCompletelyQuery}.
 */
public final class DisableThrowQuery
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;

	public DisableThrowQuery(WorldGuardPlugin worldGuardPlugin, RegionContainer regionContainer,
			SessionManager sessionManager)
	{
		this.worldGuardPlugin = worldGuardPlugin;
		this.regionContainer = regionContainer;
		this.sessionManager = sessionManager;
	}

	public void sendBlocked(Player player, Material material)
	{
		if (material != null && material != Material.AIR)
		{
			Messages.sendMessageWithCooldown(player, "disable-throw-blocked", "item", material.name());
		}
	}

	/**
	 * Material for supported throwable projectiles, or {@link Material#AIR} if not one of the four types.
	 */
	public static Material throwableMaterialFromProjectile(org.bukkit.entity.Entity entity)
	{
		if (entity instanceof Snowball)
		{
			return Material.SNOWBALL;
		}
		if (entity instanceof Egg)
		{
			return Material.EGG;
		}
		if (entity instanceof EnderPearl)
		{
			return Material.ENDER_PEARL;
		}
		if (entity instanceof ThrownExpBottle)
		{
			return Material.EXPERIENCE_BOTTLE;
		}
		return Material.AIR;
	}

	/**
	 * Prefer hand that holds the matching throwable; falls back to projectile mapping when neither matches (still valid throw).
	 */
	public static Material resolveThrownMaterial(Player player, Material projectileMaterial)
	{
		if (player == null || projectileMaterial == null || projectileMaterial == Material.AIR)
		{
			return Material.AIR;
		}
		if (!ThrowableItemFlag.isThrowableMaterial(projectileMaterial))
		{
			return Material.AIR;
		}
		PlayerInventory inv = player.getInventory();
		org.bukkit.inventory.ItemStack main = inv.getItemInMainHand();
		org.bukkit.inventory.ItemStack off = inv.getItemInOffHand();
		if (main != null && main.getType() == projectileMaterial)
		{
			return projectileMaterial;
		}
		if (off != null && off.getType() == projectileMaterial)
		{
			return projectileMaterial;
		}
		return projectileMaterial;
	}

	public boolean isDisabled(Player player, Material material)
	{
		if (player == null || material == null || material == Material.AIR || !ThrowableItemFlag.isThrowableMaterial(material))
		{
			return false;
		}
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		if (WorldGuardUtils.hasBypass(this.sessionManager, localPlayer, player))
		{
			return false;
		}
		String name = material.name();
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(WorldGuardUtils.adaptLocation(player.getLocation()));
		Set<String> set = regions.queryValue(localPlayer, Flags.DISABLE_THROW);
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
		}
		return false;
	}
}
