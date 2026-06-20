package dev.tins.worldguardextraflagsplus.wg.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.SessionManager;

import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

final class CommandFlagHandlerSupport
{
	private CommandFlagHandlerSupport()
	{
	}

	static List<Set<String>> collectCommandSets(ApplicableRegionSet regionSet, LocalPlayer player, Flag<Set<String>> flag)
	{
		List<Set<String>> commandSets = new ArrayList<>(regionSet.queryAllValues(player, flag));
		for (ProtectedRegion region : regionSet)
		{
			Set<String> regionCommands = region.getFlag(flag);
			if (regionCommands != null && !commandSets.contains(regionCommands))
			{
				commandSets.add(regionCommands);
			}
		}
		return commandSets;
	}

	static void dispatchEntryCommands(
			SessionManager sessionManager,
			LocalPlayer player,
			World world,
			Set<ProtectedRegion> enteredRegions,
			Flag<Set<String>> flag,
			Collection<Set<String>> knownCommandSets,
			boolean asConsole)
	{
		if (enteredRegions == null || enteredRegions.isEmpty())
		{
			return;
		}

		if (sessionManager.hasBypass(player, world))
		{
			return;
		}

		if (!WorldGuardUtils.isPluginEnabled())
		{
			return;
		}

		Player bukkitPlayer = null;
		if (!asConsole)
		{
			if (!(player instanceof BukkitPlayer bukkitPlayerWrapper))
			{
				return;
			}

			bukkitPlayer = bukkitPlayerWrapper.getPlayer();
			if (bukkitPlayer == null || !bukkitPlayer.isOnline())
			{
				return;
			}
		}

		for (ProtectedRegion region : enteredRegions)
		{
			Set<String> commandSet = region.getFlag(flag);
			if (commandSet == null || commandSet.isEmpty())
			{
				continue;
			}

			if (knownCommandSets.contains(commandSet))
			{
				continue;
			}

			dispatchCommandSet(player, bukkitPlayer, commandSet, asConsole);
		}
	}

	static void dispatchEntryCommandsForApplicableSet(
			SessionManager sessionManager,
			LocalPlayer player,
			World world,
			ApplicableRegionSet regionSet,
			Flag<Set<String>> flag,
			Collection<Set<String>> knownCommandSets,
			boolean asConsole)
	{
		Set<ProtectedRegion> regionsWithFlag = new HashSet<>();
		for (ProtectedRegion region : regionSet)
		{
			Set<String> commandSet = region.getFlag(flag);
			if (commandSet != null && !commandSet.isEmpty())
			{
				regionsWithFlag.add(region);
			}
		}

		dispatchEntryCommands(sessionManager, player, world, regionsWithFlag, flag, knownCommandSets, asConsole);
	}

	static void dispatchExitCommands(
			SessionManager sessionManager,
			LocalPlayer player,
			World world,
			Collection<Set<String>> previousCommandSets,
			Collection<Set<String>> currentCommandSets,
			boolean asConsole)
	{
		if (sessionManager.hasBypass(player, world))
		{
			return;
		}

		if (!WorldGuardUtils.isPluginEnabled())
		{
			return;
		}

		Player bukkitPlayer = null;
		if (!asConsole)
		{
			if (!(player instanceof BukkitPlayer bukkitPlayerWrapper))
			{
				return;
			}

			bukkitPlayer = bukkitPlayerWrapper.getPlayer();
			if (bukkitPlayer == null || !bukkitPlayer.isOnline())
			{
				return;
			}
		}

		for (Set<String> previousSet : previousCommandSets)
		{
			if (previousSet == null || previousSet.isEmpty())
			{
				continue;
			}

			if (currentCommandSets.contains(previousSet))
			{
				continue;
			}

			dispatchCommandSet(player, bukkitPlayer, previousSet, asConsole);
		}
	}

	private static void dispatchCommandSet(LocalPlayer player, Player bukkitPlayer, Set<String> commandSet, boolean asConsole)
	{
		for (String command : commandSet)
		{
			String processedCommand = CommandPlaceholderUtil.prepareForDispatch(player, command);
			if (processedCommand.isEmpty())
			{
				continue;
			}

			if (asConsole)
			{
				scheduleConsoleCommand(processedCommand);
			}
			else
			{
				schedulePlayerCommand(bukkitPlayer, processedCommand);
			}
		}
	}

	private static void schedulePlayerCommand(Player player, String command)
	{
		WorldGuardUtils.getScheduler().runAtEntity(player, task ->
				Bukkit.getServer().dispatchCommand(player, command));
	}

	private static void scheduleConsoleCommand(String command)
	{
		CommandSender console = Bukkit.getServer().getConsoleSender();
		WorldGuardUtils.getScheduler().runNextTick(task ->
				Bukkit.getServer().dispatchCommand(console, command));
	}
}
