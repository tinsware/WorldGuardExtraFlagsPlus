package dev.tins.worldguardextraflagsplus.listeners;

import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;

import dev.tins.worldguardextraflagsplus.Config;
import dev.tins.worldguardextraflagsplus.flags.Flags;

final class BucketAllowSupport
{
	enum AllowDecision
	{
		ALLOW,
		DENY,
		NO_MATCH
	}

	static final class BucketMaterials
	{
		private final Material host;
		private final Material liquid;

		BucketMaterials(Material host, Material liquid)
		{
			this.host = host;
			this.liquid = liquid;
		}

		Material getHost()
		{
			return host;
		}

		Material getLiquid()
		{
			return liquid;
		}
	}

	private BucketAllowSupport()
	{
	}

	static BucketMaterials resolveBucketMaterials(Block block)
	{
		if (block == null)
		{
			return new BucketMaterials(null, null);
		}

		Material host = block.getType();
		Material liquid = null;

		if (host == Material.WATER || host == Material.LAVA)
		{
			liquid = host;
		}
		else
		{
			BlockData blockData = block.getBlockData();
			if (blockData instanceof Waterlogged waterlogged && waterlogged.isWaterlogged())
			{
				liquid = Material.WATER;
			}
		}

		return new BucketMaterials(host, liquid);
	}

	static AllowDecision evaluateBreakAllow(LocalPlayer localPlayer, ApplicableRegionSet regions, BucketMaterials materials)
	{
		return evaluateAllow(
				localPlayer,
				regions,
				materials,
				regions.queryValue(localPlayer, Flags.DENY_BLOCK_BREAK),
				(localPlayerArg, regionsArg, material) ->
						BlockAllowMembershipSupport.isAllowBlockBreakAllowed(localPlayerArg, regionsArg, material));
	}

	static AllowDecision evaluatePlaceAllow(LocalPlayer localPlayer, ApplicableRegionSet regions, BucketMaterials materials)
	{
		return evaluateAllow(
				localPlayer,
				regions,
				materials,
				regions.queryValue(localPlayer, Flags.DENY_BLOCK_PLACE),
				(localPlayerArg, regionsArg, material) ->
						BlockAllowMembershipSupport.isAllowBlockPlaceAllowed(localPlayerArg, regionsArg, material));
	}

	static AllowDecision evaluatePlaceAllow(LocalPlayer localPlayer, ApplicableRegionSet regions, Material liquidMaterial)
	{
		return evaluatePlaceAllow(
				localPlayer,
				regions,
				new BucketMaterials(liquidMaterial, liquidMaterial));
	}

	private static AllowDecision evaluateAllow(
			LocalPlayer localPlayer,
			ApplicableRegionSet regions,
			BucketMaterials materials,
			Set<Material> denySet,
			AllowMembershipChecker allowChecker)
	{
		if (regions == null || materials.getHost() == null)
		{
			return AllowDecision.NO_MATCH;
		}

		Predicate<Material> isDenied = material ->
				material != null && denySet != null && !denySet.isEmpty() && denySet.contains(material);
		Predicate<Material> isAllowed = material ->
				material != null && allowChecker.isAllowed(localPlayer, regions, material);

		if (Config.isDenyFirst())
		{
			if (matchesAnyCheckMaterial(materials, isDenied))
			{
				return AllowDecision.DENY;
			}

			if (matchesAnyCheckMaterial(materials, isAllowed))
			{
				return AllowDecision.ALLOW;
			}
		}
		else
		{
			if (matchesAnyCheckMaterial(materials, isAllowed))
			{
				return AllowDecision.ALLOW;
			}

			if (matchesAnyCheckMaterial(materials, isDenied))
			{
				return AllowDecision.DENY;
			}
		}

		return AllowDecision.NO_MATCH;
	}

	private static boolean matchesAnyCheckMaterial(BucketMaterials materials, Predicate<Material> predicate)
	{
		String checkMode = Config.getWaterloggedMaterialCheck();

		if ("host".equalsIgnoreCase(checkMode))
		{
			return predicate.test(materials.getHost());
		}

		if ("liquid".equalsIgnoreCase(checkMode))
		{
			Material liquid = resolveLiquidForCheck(materials);
			return liquid != null && predicate.test(liquid);
		}

		// both (default)
		if (predicate.test(materials.getHost()))
		{
			return true;
		}

		Material liquid = resolveLiquidForCheck(materials);
		return liquid != null && liquid != materials.getHost() && predicate.test(liquid);
	}

	private static Material resolveLiquidForCheck(BucketMaterials materials)
	{
		if (materials.getLiquid() != null)
		{
			return materials.getLiquid();
		}

		Material host = materials.getHost();
		if (host == Material.WATER || host == Material.LAVA)
		{
			return host;
		}

		return null;
	}

	@FunctionalInterface
	private interface AllowMembershipChecker
	{
		boolean isAllowed(LocalPlayer localPlayer, ApplicableRegionSet regions, Material material);
	}
}
