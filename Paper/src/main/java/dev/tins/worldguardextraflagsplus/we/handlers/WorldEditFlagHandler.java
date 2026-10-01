package dev.tins.worldguardextraflagsplus.we.handlers;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;

import dev.tins.worldguardextraflagsplus.Messages;
import dev.tins.worldguardextraflagsplus.flags.Flags;

/**
 * Wraps a WorldEdit {@link Extent} to enforce WorldGuard {@link Flags#WORLDEDIT}.
 * <p>
 * Vanilla WorldEdit routes bulk edits through {@link #setBlock(BlockVector3, BlockStateHolder)} via patterns.
 * FastAsyncWorldEdit additionally calls batch methods ({@link #setBlocks}, {@link #replaceBlocks}, coordinate
 * {@link #setBlock} overloads); those are overridden here so FAWE cannot bypass the flag.
 */
public final class WorldEditFlagHandler extends AbstractDelegateExtent
{
	/** Staff bypass for {@code worldedit} DENY regions (FAWE/WE). */
	public static final String BYPASS_PERMISSION = "worldguardextraflagsplus.worldedit.bypass";
	/** If true, players do not receive {@link Messages} when an edit is denied. */
	public static final String SILENT_DENY_PERMISSION = "worldguardextraflagsplus.worldedit.silent-deny";

	private final LocalPlayer player;
	private final RegionManager regionManager;

	public WorldEditFlagHandler(Extent extent, LocalPlayer player, RegionManager regionManager)
	{
		super(extent);
		this.player = player;
		this.regionManager = regionManager;
	}

	private boolean hasBypass()
	{
		if (!(this.player instanceof BukkitPlayer bp))
		{
			return false;
		}
		return bp.getPlayer().hasPermission(BYPASS_PERMISSION);
	}

	private boolean isWorldEditAllowed(BlockVector3 position)
	{
		ApplicableRegionSet regions = this.regionManager.getApplicableRegions(position);
		return regions.queryState(this.player, Flags.WORLDEDIT) != State.DENY;
	}

	private boolean canEdit(BlockVector3 position)
	{
		return this.hasBypass() || this.isWorldEditAllowed(position);
	}

	private boolean canEditRegion(@NotNull Region region)
	{
		if (this.hasBypass())
		{
			return true;
		}
		for (BlockVector3 pos : region)
		{
			if (!this.isWorldEditAllowed(pos))
			{
				return false;
			}
		}
		return true;
	}

	private boolean canEditVectors(@NotNull Set<BlockVector3> positions)
	{
		if (this.hasBypass())
		{
			return true;
		}
		for (BlockVector3 pos : positions)
		{
			if (!this.isWorldEditAllowed(pos))
			{
				return false;
			}
		}
		return true;
	}

	private void notifyDeniedOnce()
	{
		if (!(this.player instanceof BukkitPlayer bp))
		{
			return;
		}
		if (bp.getPlayer().hasPermission(SILENT_DENY_PERMISSION))
		{
			return;
		}
		Messages.sendMessageWithCooldown(bp.getPlayer(), "worldedit-denied");
	}

	@Override
	public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException
	{
		if (this.canEdit(location))
		{
			return super.setBlock(location, block);
		}
		this.notifyDeniedOnce();
		return false;
	}

	@Override
	public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException
	{
		return this.setBlock(BlockVector3.at(x, y, z), block);
	}

	@Override
	public int setBlocks(@NotNull Region region, Pattern pattern) throws MaxChangedBlocksException
	{
		if (this.canEditRegion(region))
		{
			return super.setBlocks(region, pattern);
		}
		this.notifyDeniedOnce();
		return 0;
	}

	@Override
	public <B extends BlockStateHolder<B>> int setBlocks(@NotNull Region region, B block) throws MaxChangedBlocksException
	{
		if (this.canEditRegion(region))
		{
			return super.setBlocks(region, block);
		}
		this.notifyDeniedOnce();
		return 0;
	}

	@Override
	public int setBlocks(@NotNull Set<BlockVector3> positions, Pattern pattern)
	{
		if (this.canEditVectors(positions))
		{
			return super.setBlocks(positions, pattern);
		}
		this.notifyDeniedOnce();
		return 0;
	}

	@Override
	public int replaceBlocks(@NotNull Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException
	{
		if (this.canEditRegion(region))
		{
			return super.replaceBlocks(region, mask, pattern);
		}
		this.notifyDeniedOnce();
		return 0;
	}

	@Override
	public <B extends BlockStateHolder<B>> int replaceBlocks(@NotNull Region region, Set<BaseBlock> filter, B replacement)
			throws MaxChangedBlocksException
	{
		if (this.canEditRegion(region))
		{
			return super.replaceBlocks(region, filter, replacement);
		}
		this.notifyDeniedOnce();
		return 0;
	}

	@Override
	public int replaceBlocks(@NotNull Region region, Set<BaseBlock> filter, Pattern pattern)
			throws MaxChangedBlocksException
	{
		if (this.canEditRegion(region))
		{
			return super.replaceBlocks(region, filter, pattern);
		}
		this.notifyDeniedOnce();
		return 0;
	}

	@Override
	public boolean setBiome(BlockVector3 position, BiomeType biome)
	{
		if (this.canEdit(position))
		{
			return super.setBiome(position, biome);
		}
		this.notifyDeniedOnce();
		return false;
	}

	@Override
	public boolean setBiome(int x, int y, int z, BiomeType biome)
	{
		return this.setBiome(BlockVector3.at(x, y, z), biome);
	}
}
