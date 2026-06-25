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
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.data.SoundData;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

public class PlaySoundsFlagHandler extends FlagValueChangeHandler<Set<SoundData>>
{
	public static final Factory FACTORY(Plugin plugin)
	{
		return new Factory(plugin);
	}
	
    public static class Factory extends Handler.Factory<PlaySoundsFlagHandler>
    {
		private final Plugin plugin;

		public Factory(Plugin plugin)
		{
			this.plugin = plugin;
		}

		@Override
        public PlaySoundsFlagHandler create(Session session)
        {
            return new PlaySoundsFlagHandler(this.plugin, session);
        }
    }

	private final Plugin plugin;
    private Map<String, WrappedRunnable> runnables;
	    
	protected PlaySoundsFlagHandler(Plugin plugin, Session session)
	{
		super(session, Flags.PLAY_SOUNDS);

		this.plugin = plugin;
		
		this.runnables = new ConcurrentHashMap<>();
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Set<SoundData> value)
	{
		this.handleValue(player, value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<SoundData> currentValue, Set<SoundData> lastValue, MoveType moveType)
	{
		this.handleValue(player, currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<SoundData> lastValue, MoveType moveType)
	{
		this.handleValue(player, null);
		return true;
	}

	@Override
	public void tick(LocalPlayer player, ApplicableRegionSet set)
	{
		this.handleValue(player, set.queryValue(player, Flags.PLAY_SOUNDS));
    }
	
	private void handleValue(LocalPlayer player, Set<SoundData> value)
	{
		if ((value == null || value.isEmpty()) && this.runnables.isEmpty())
		{
			return;
		}

		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();

		if (value != null && value.size() > 0)
		{
			for(SoundData sound : value)
			{
				if (!this.runnables.containsKey(sound.sound()))
				{
					WrappedRunnable runnable = new WrappedRunnable()
					{
						private WrappedTask wrappedTask;

						@Override
						public void run()
						{
							bukkitPlayer.playSound(bukkitPlayer.getLocation(), sound.sound(), sound.source(), sound.volume(), sound.pitch());
						}

						@Override
						public void cancel()
						{
							if (wrappedTask != null)
							{
								wrappedTask.cancel();
							}

							bukkitPlayer.stopSound(sound.sound(), sound.source());
						}

						@Override
						public void setWrappedTask(WrappedTask wrappedTask)
						{
							this.wrappedTask = wrappedTask;
						}
					};

					WrappedTask wrappedTask = WorldGuardUtils.getScheduler().runAtEntityTimer(
						bukkitPlayer,
						runnable,
						1L,
						sound.interval() * 50L,
						TimeUnit.MILLISECONDS
					);
					runnable.setWrappedTask(wrappedTask);

					this.runnables.put(sound.sound(), runnable);
				}
			}
		}
		
		Iterator<Entry<String, WrappedRunnable>> runnables = this.runnables.entrySet().iterator();
		while (runnables.hasNext())
		{
			Entry<String, WrappedRunnable> runnable = runnables.next();
			
			if (value != null && value.size() > 0)
			{
				boolean skip = false;
				for(SoundData sound : value)
				{
					if (sound.sound().equals(runnable.getKey()))
					{
						skip = true;
						break;
					}
				}
				
				if (skip)
				{
					continue;
				}
			}
			
			runnable.getValue().cancel();
			
			runnables.remove();
		}
	}

	interface WrappedRunnable extends Runnable
	{
		void cancel();

		@Override
		void run();

		void setWrappedTask(WrappedTask wrappedTask);
	}
}



