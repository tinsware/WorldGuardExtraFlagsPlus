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

public class ConsoleCommandOnEntryFlagHandler extends Handler
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}

	public static class Factory extends Handler.Factory<ConsoleCommandOnEntryFlagHandler>
	{
		@Override
		public ConsoleCommandOnEntryFlagHandler create(Session session)
		{
			return new ConsoleCommandOnEntryFlagHandler(session);
		}
	}

	private List<Set<String>> lastCommands;

	protected ConsoleCommandOnEntryFlagHandler(Session session)
	{
		super(session);

		this.lastCommands = List.of();
	}

	@Override
	public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set)
	{
		CommandFlagHandlerSupport.dispatchEntryCommandsForApplicableSet(
				this.getSession().getManager(),
				player,
				(World) current.getExtent(),
				set,
				Flags.CONSOLE_COMMAND_ON_ENTRY,
				this.lastCommands,
				true);
		this.lastCommands = CommandFlagHandlerSupport.collectCommandSets(set, player, Flags.CONSOLE_COMMAND_ON_ENTRY);
	}

	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
			Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		CommandFlagHandlerSupport.dispatchEntryCommands(
				this.getSession().getManager(),
				player,
				(World) to.getExtent(),
				entered,
				Flags.CONSOLE_COMMAND_ON_ENTRY,
				this.lastCommands,
				true);
		this.lastCommands = CommandFlagHandlerSupport.collectCommandSets(toSet, player, Flags.CONSOLE_COMMAND_ON_ENTRY);
		return true;
	}
}
