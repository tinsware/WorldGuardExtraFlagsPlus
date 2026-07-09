package dev.tins.worldguardextraflagsplus.wg.handlers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the {@code console-command-repeat} flag.
 *
 * <p>Each entry in the flag set has the format {@code <seconds> <command>}. The handler
 * validates that the interval is between 1 and 60 seconds, then schedules a repeating
 * Folia-safe task on the player's entity thread that dispatches the command from console
 * with placeholder replacement.</p>
 *
 * <p>When the player leaves all applicable regions (or the flag is removed), all
 * timers are cancelled. On disconnect, all timers are cleaned up.</p>
 */
public class ConsoleCommandRepeatFlagHandler extends FlagValueChangeHandler<Set<String>>
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}

	public static class Factory extends Handler.Factory<ConsoleCommandRepeatFlagHandler>
	{
		@Override
		public ConsoleCommandRepeatFlagHandler create(Session session)
		{
			return new ConsoleCommandRepeatFlagHandler(session);
		}
	}

	/**
	 * Map of entry string -> WrappedRunnable for each active repeating task.
	 * Key is the raw flag entry (e.g. "20 give %player% diamond 1") so we can
	 * identify and cancel individual entries when the set changes.
	 */
	private final Map<String, WrappedRunnable> runnables;

	protected ConsoleCommandRepeatFlagHandler(Session session)
	{
		super(session, Flags.CONSOLE_COMMAND_REPEAT);

		this.runnables = new ConcurrentHashMap<>();
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Set<String> value)
	{
		this.handleValue(player, value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
			Set<String> currentValue, Set<String> lastValue, MoveType moveType)
	{
		this.handleValue(player, currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
			Set<String> lastValue, MoveType moveType)
	{
		this.handleValue(player, null);
		return true;
	}

	@Override
	public void tick(LocalPlayer player, ApplicableRegionSet set)
	{
		this.handleValue(player, set.queryValue(player, Flags.CONSOLE_COMMAND_REPEAT));
	}

	// -------------------------------------------------------------------------
	// Core logic
	// -------------------------------------------------------------------------

	/**
	 * Compares the new value set against currently running timers and starts or
	 * stops entries as needed.
	 */
	private void handleValue(LocalPlayer player, Set<String> value)
	{
		if ((value == null || value.isEmpty()) && this.runnables.isEmpty())
		{
			return;
		}

		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();
		if (bukkitPlayer == null || !bukkitPlayer.isOnline())
		{
			return;
		}

		if (value != null && !value.isEmpty())
		{
			for (String entry : value)
			{
				if (entry == null || entry.isEmpty() || this.runnables.containsKey(entry))
				{
					continue;
				}

				RepeatingCommand parsed = parseEntry(entry);
				if (parsed == null)
				{
					continue; // Invalid entry, skip silently
				}

				WrappedRunnable runnable = new WrappedRunnable()
				{
					private WrappedTask wrappedTask;

					@Override
					public void run()
					{
						String processed = CommandPlaceholderUtil.prepareForDispatch(player, parsed.command);
						if (!processed.isEmpty())
						{
							CommandSender console = Bukkit.getServer().getConsoleSender();
							Bukkit.getServer().dispatchCommand(console, processed);
						}
					}

					@Override
					public void cancel()
					{
						if (wrappedTask != null)
						{
							wrappedTask.cancel();
						}
					}

					@Override
					public void setWrappedTask(WrappedTask task)
					{
						this.wrappedTask = task;
					}
				};

				WrappedTask task = WorldGuardUtils.getScheduler().runAtEntityTimer(
						bukkitPlayer,
						runnable,
						0L,
						parsed.intervalMillis,
						TimeUnit.MILLISECONDS);
				runnable.setWrappedTask(task);

				this.runnables.put(entry, runnable);
			}
		}

		// Cancel any running timers whose entry is no longer in the set
		Iterator<Entry<String, WrappedRunnable>> iterator = this.runnables.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<String, WrappedRunnable> entry = iterator.next();

			if (value != null && value.contains(entry.getKey()))
			{
				continue;
			}

			entry.getValue().cancel();
			iterator.remove();
		}
	}

	// -------------------------------------------------------------------------
	// Entry parsing
	// -------------------------------------------------------------------------

	/**
	 * Parses a flag entry of the format {@code "<seconds> <command>"}.
	 *
	 * @param entry the raw flag value
	 * @return parsed data, or {@code null} if the entry is invalid
	 */
	private static RepeatingCommand parseEntry(String entry)
	{
		if (entry == null || entry.isEmpty())
		{
			return null;
		}

		int firstSpace = entry.indexOf(' ');
		if (firstSpace <= 0 || firstSpace >= entry.length() - 1)
		{
			return null;
		}

		String secondsStr = entry.substring(0, firstSpace).trim();
		String command = entry.substring(firstSpace + 1).trim();

		if (command.isEmpty())
		{
			return null;
		}

		int seconds;
		try
		{
			seconds = Integer.parseInt(secondsStr);
		}
		catch (NumberFormatException e)
		{
			return null;
		}

		if (seconds < 1 || seconds > 60)
		{
			return null;
		}

		return new RepeatingCommand(command, seconds * 1000L);
	}

	/**
	 * Holds the parsed command and pre-computed interval in milliseconds.
	 */
	private record RepeatingCommand(String command, long intervalMillis) {}

	// -------------------------------------------------------------------------
	// Runnable wrapper (same pattern as PlaySoundsFlagHandler)
	// -------------------------------------------------------------------------

	interface WrappedRunnable extends Runnable
	{
		void cancel();

		@Override
		void run();

		void setWrappedTask(WrappedTask wrappedTask);
	}
}
