package dev.tins.worldguardextraflagsplus.wg.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;

import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

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

	private Collection<Set<String>> lastCommands;

	protected ConsoleCommandOnEntryFlagHandler(Session session)
	{
		super(session);

		this.lastCommands = new ArrayList<>();
	}

	@Override
	public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set)
	{
		Collection<Set<String>> commands = set.queryAllValues(player, Flags.CONSOLE_COMMAND_ON_ENTRY);
		this.tryDispatch(player, (World) current.getExtent(), commands);
		this.finishLastCommandsTracking(set, commands);
	}

	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		Collection<Set<String>> commands = toSet.queryAllValues(player, Flags.CONSOLE_COMMAND_ON_ENTRY);
		this.tryDispatch(player, (World) to.getExtent(), commands);
		this.finishLastCommandsTracking(toSet, commands);
		return true;
	}

	private void tryDispatch(LocalPlayer player, World world, Collection<Set<String>> commands)
	{
		if (this.getSession().getManager().hasBypass(player, world))
		{
			return;
		}

		for (Set<String> commands_ : commands)
		{
			if (!this.lastCommands.contains(commands_) && !commands_.isEmpty())
			{
				for (String command : commands_)
				{
					String processedCommand = CommandPlaceholderUtil.prepareForDispatch(player, command);
					if (processedCommand.isEmpty())
					{
						continue;
					}
					WorldGuardUtils.getScheduler().runNextTick((wrappedTask) ->
						Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), processedCommand));
				}

				break;
			}
		}
	}

	private void finishLastCommandsTracking(ApplicableRegionSet toSet, Collection<Set<String>> commands)
	{
		this.lastCommands = new ArrayList<>(commands);

		if (!this.lastCommands.isEmpty())
		{
			for (ProtectedRegion region : toSet)
			{
				Set<String> commands_ = region.getFlag(Flags.CONSOLE_COMMAND_ON_ENTRY);
				if (commands_ != null)
				{
					this.lastCommands.add(commands_);
				}
			}
		}
	}
}
