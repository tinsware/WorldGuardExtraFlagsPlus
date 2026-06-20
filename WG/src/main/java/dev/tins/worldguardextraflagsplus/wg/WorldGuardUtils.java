package dev.tins.worldguardextraflagsplus.wg;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;

public class WorldGuardUtils
{
	public static final String PREVENT_TELEPORT_LOOP_META = "WGEFP: TLP";
	
	private static FoliaLib foliaLib;
	private static SchedulerWrapper schedulerWrapper;
	private static Plugin plugin;
	
	public static void initializeScheduler(Plugin plugin)
	{
		WorldGuardUtils.foliaLib = new FoliaLib(plugin);
		WorldGuardUtils.schedulerWrapper = new SchedulerWrapper(foliaLib);
		WorldGuardUtils.plugin = plugin;

		verifySchedulerLoaded(plugin);
	}

	private static void verifySchedulerLoaded(Plugin plugin)
	{
		if (schedulerWrapper == null)
		{
			plugin.getLogger().severe("[Scheduler] FoliaLib failed to initialize — flag schedulers will not run.");
			return;
		}

		try
		{
			schedulerWrapper.runNextTick(task -> { });
		}
		catch (Throwable throwable)
		{
			plugin.getLogger().severe("[Scheduler] FoliaLib task scheduling failed — command/console-on-entry and other "
					+ "scheduled flags will not work. Rebuild the plugin with mvn package (shaded jar). Cause: "
					+ (throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName()));
		}
	}
	
	public static SchedulerWrapper getScheduler()
	{
		return schedulerWrapper;
	}

	public static void cancelAllTasks()
	{
		if (foliaLib != null)
		{
			foliaLib.getScheduler().cancelAllTasks();
		}
	}
	
	public static boolean isPluginEnabled()
	{
		return plugin != null && plugin.isEnabled();
	}
	
	public static class SchedulerWrapper
	{
		private final FoliaLib foliaLib;
		private final com.tcoded.folialib.impl.ServerImplementation scheduler;
		
		private SchedulerWrapper(FoliaLib foliaLib)
		{
			this.foliaLib = foliaLib;
			this.scheduler = foliaLib.getScheduler();
		}
		
		public FoliaLib getImpl()
		{
			return foliaLib;
		}
		
		/**
		 * Returns the scheduler implementation.
		 * According to FoliaLib API: foliaLib.getScheduler() returns ServerImplementation
		 * which provides methods like runAtEntity(), runNextTick(), etc.
		 * 
		 * Note: This exposes the internal ServerImplementation type. For better API design,
		 * prefer using the wrapper methods below (runAtEntity, runNextTick, etc.) instead.
		 * 
		 * @return The ServerImplementation instance from FoliaLib
		 */
		public com.tcoded.folialib.impl.ServerImplementation getScheduler()
		{
			return scheduler;
		}
		
		/**
		 * Runs a task at the entity's thread.
		 * On Folia: runs on EntityScheduler (appropriate for the entity)
		 * On Spigot/Paper: runs on main thread
		 * 
		 * @param entity The entity to run the task for
		 * @param task The task to run (Consumer<WrappedTask>)
		 */
		public void runAtEntity(org.bukkit.entity.Entity entity, java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask> task)
		{
			scheduler.runAtEntity(entity, task);
		}
		
		/**
		 * Runs a task on the next tick.
		 * On Folia: runs on GlobalRegionScheduler (for world operations, NOT player-specific)
		 * On Spigot/Paper: runs on main thread
		 * 
		 * Warning: On Folia, this should NOT be used for player/entity operations.
		 * Use runAtEntity() for player/entity-specific code.
		 * 
		 * @param task The task to run (Consumer<WrappedTask>)
		 */
		public void runNextTick(java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask> task)
		{
			scheduler.runNextTick(task);
		}
		
		/**
		 * Runs a repeating task at the entity's thread.
		 * On Folia: runs on EntityScheduler
		 * On Spigot/Paper: runs on main thread
		 * 
		 * @param entity The entity to run the task for
		 * @param runnable The task to run
		 * @param delayTicks Delay before first execution (in ticks)
		 * @param periodTicks Period between executions (in ticks)
		 * @return WrappedTask that can be cancelled
		 */
		public com.tcoded.folialib.wrapper.task.WrappedTask runAtEntityTimer(org.bukkit.entity.Entity entity, java.lang.Runnable runnable, long delayTicks, long periodTicks)
		{
			return scheduler.runAtEntityTimer(entity, runnable, delayTicks, periodTicks);
		}
		
		/**
		 * Runs a repeating task at the entity's thread with TimeUnit support.
		 * On Folia: runs on EntityScheduler
		 * On Spigot/Paper: runs on main thread
		 * 
		 * @param entity The entity to run the task for
		 * @param runnable The task to run
		 * @param delay Delay before first execution
		 * @param period Period between executions
		 * @param timeUnit TimeUnit for delay and period
		 * @return WrappedTask that can be cancelled
		 */
		public com.tcoded.folialib.wrapper.task.WrappedTask runAtEntityTimer(org.bukkit.entity.Entity entity, java.lang.Runnable runnable, long delay, long period, java.util.concurrent.TimeUnit timeUnit)
		{
			return scheduler.runAtEntityTimer(entity, runnable, delay, period, timeUnit);
		}

		/**
		 * Runs a task on the region thread that owns the given location/chunk.
		 * On Spigot/Paper: runs on the main thread.
		 */
		public void runAtLocation(org.bukkit.Location location, java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask> task)
		{
			scheduler.runAtLocation(location, task);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static boolean hasNoTeleportLoop(Plugin plugin, Player player, Object location)
	{
		MetadataValue result = player.getMetadata(WorldGuardUtils.PREVENT_TELEPORT_LOOP_META).stream()
				.filter((p) -> p.getOwningPlugin().equals(plugin))
				.findFirst()
				.orElse(null);
		
		if (result == null)
		{
			result = new FixedMetadataValue(plugin, new HashSet<>());
			
			player.setMetadata(WorldGuardUtils.PREVENT_TELEPORT_LOOP_META, result);
			
			if (schedulerWrapper != null && foliaLib != null)
			{
				schedulerWrapper.runAtEntity(player, (wrappedTask) ->
				{
					player.removeMetadata(WorldGuardUtils.PREVENT_TELEPORT_LOOP_META, plugin);
				});
			}
			else
			{
				// Fallback if scheduler not initialized
				plugin.getServer().getScheduler().runTask(plugin, () ->
				{
					player.removeMetadata(WorldGuardUtils.PREVENT_TELEPORT_LOOP_META, plugin);
				});
			}
		}
		
		Set<Object> set = (Set<Object>)result.value();
		if (set.add(location))
		{
			return true;
		}
		
		return false;
	}
}



