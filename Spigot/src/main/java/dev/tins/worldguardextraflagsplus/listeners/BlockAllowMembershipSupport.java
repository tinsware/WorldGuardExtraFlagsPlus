package dev.tins.worldguardextraflagsplus.listeners;

import java.util.Set;

import org.bukkit.Material;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.tins.worldguardextraflagsplus.Config;

final class BlockAllowMembershipSupport
{
	private BlockAllowMembershipSupport()
	{
	}

	static boolean isMaterialAllowed(
			LocalPlayer localPlayer,
			ApplicableRegionSet regions,
			Material material,
			Flag<Set<Material>> allowFlag,
			boolean requireMembership)
	{
		if (material == null || regions == null)
		{
			return false;
		}

		if (!requireMembership)
		{
			Set<Material> allowSet = regions.queryValue(localPlayer, allowFlag);
			return allowSet != null && !allowSet.isEmpty() && allowSet.contains(material);
		}

		for (ProtectedRegion region : regions)
		{
			Set<Material> allowSet = region.getFlag(allowFlag);
			if (allowSet == null || allowSet.isEmpty() || !allowSet.contains(material))
			{
				continue;
			}

			if (region.isOwner(localPlayer) || region.isMember(localPlayer))
			{
				return true;
			}
		}

		return false;
	}

	static boolean isAllowBlockPlaceAllowed(LocalPlayer localPlayer, ApplicableRegionSet regions, Material material)
	{
		return isMaterialAllowed(
				localPlayer,
				regions,
				material,
				dev.tins.worldguardextraflagsplus.flags.Flags.ALLOW_BLOCK_PLACE,
				Config.isAllowBlockPlaceRequireMembership());
	}

	static boolean isAllowBlockBreakAllowed(LocalPlayer localPlayer, ApplicableRegionSet regions, Material material)
	{
		return isMaterialAllowed(
				localPlayer,
				regions,
				material,
				dev.tins.worldguardextraflagsplus.flags.Flags.ALLOW_BLOCK_BREAK,
				Config.isAllowBlockBreakRequireMembership());
	}

	static boolean isAllowBlockBreakAllowed(ApplicableRegionSet regions, Material material)
	{
		return isMaterialAllowed(
				null,
				regions,
				material,
				dev.tins.worldguardextraflagsplus.flags.Flags.ALLOW_BLOCK_BREAK,
				Config.isAllowBlockBreakRequireMembership());
	}
}
