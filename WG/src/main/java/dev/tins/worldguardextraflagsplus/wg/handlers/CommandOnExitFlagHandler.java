package dev.tins.worldguardextraflagsplus.wg.handlers;

import java.util.List;
import java.util.Set;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.Handler;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;

import dev.tins.worldguardextraflagsplus.flags.Flags;

public class CommandOnExitFlagHandler extends Handler
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}

	public static class Factory extends Handler.Factory<CommandOnExitFlagHandler>
	{
		@Override
		public CommandOnExitFlagHandler create(Session session)
		{
			return new CommandOnExitFlagHandler(session);
		}
	}

	private List<Set<String>> lastCommands;

	protected CommandOnExitFlagHandler(Session session)
	{
		super(session);

		this.lastCommands = List.of();
	}

	@Override
	public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set)
	{
		this.lastCommands = CommandFlagHandlerSupport.collectCommandSets(set, player, Flags.COMMAND_ON_EXIT);
	}

	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
			Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		List<Set<String>> currentCommands = CommandFlagHandlerSupport.collectCommandSets(toSet, player, Flags.COMMAND_ON_EXIT);
		List<Set<String>> previousCommands = this.lastCommands;
		this.lastCommands = currentCommands;

		CommandFlagHandlerSupport.dispatchExitCommands(
				this.getSession().getManager(),
				player,
				(World) to.getExtent(),
				previousCommands,
				currentCommands,
				false);
		return true;
	}
}
