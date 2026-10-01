package dev.tins.worldguardextraflagsplus.papi;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion exposing {@code %wgefp_players_in_region_<regionId>%} — the number of
 * online players currently inside any region with the given id, summed across all worlds.
 *
 * <p>Counts are computed once per second (every 20 ticks). Per-entity tasks run through FoliaLib's
 * scheduler so {@code Player#getLocation()} is only ever touched on the entity's owning region
 * thread (Folia-safe). Requests only read a cached snapshot, never region data.</p>
 */
public final class WGEFPPlaceholderExpansion extends PlaceholderExpansion
{
	private static final String PLACEHOLDER_PREFIX = "players_in_region_";
	private static final long REFRESH_DELAY_TICKS = 20L;
	private static final long REFRESH_PERIOD_TICKS = 20L;

	private final WorldGuardExtraFlagsPlusPlugin plugin;
	private final AtomicBoolean started = new AtomicBoolean(false);
	private final AtomicBoolean cycleRunning = new AtomicBoolean(false);

	/**
	 * Snapshot served to placeholder requests. Swapped once per refresh cycle so readers never
	 * observe a partially repopulated map.
	 */
	private volatile Map<String, Integer> currentCounts = new ConcurrentHashMap<>();

	/**
	 * Map being populated by the in-flight refresh cycle.
	 */
	private ConcurrentMap<String, Integer> buildingCounts = new ConcurrentHashMap<>();

	private volatile WrappedTask refreshTask;

	public WGEFPPlaceholderExpansion(WorldGuardExtraFlagsPlusPlugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public String getIdentifier()
	{
		return "wgefp";
	}

	@Override
	public String getAuthor()
	{
		java.util.List<String> authors = this.plugin.getDescription().getAuthors();
		return authors.isEmpty() ? "tins" : String.join(", ", authors);
	}

	@Override
	public String getVersion()
	{
		return this.plugin.getDescription().getVersion();
	}

	@Override
	public String getRequiredPlugin()
	{
		return this.plugin.getName();
	}

	@Override
	public String onRequest(OfflinePlayer player, String identifier)
	{
		return resolve(identifier);
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier)
	{
		return resolve(identifier);
	}

	private String resolve(String identifier)
	{
		if (identifier == null || !identifier.startsWith(PLACEHOLDER_PREFIX))
		{
			return null;
		}
		String regionId = identifier.substring(PLACEHOLDER_PREFIX.length()).toLowerCase(Locale.ROOT);
		if (regionId.isEmpty())
		{
			return "0";
		}
		Integer count = this.currentCounts.get(regionId);
		return count != null ? String.valueOf(count) : "0";
	}

	public void start()
	{
		if (!this.started.compareAndSet(false, true))
		{
			return;
		}
		this.refreshTask = WorldGuardUtils.getScheduler().getScheduler()
				.runTimer(this::runRefreshCycle, REFRESH_DELAY_TICKS, REFRESH_PERIOD_TICKS);
	}

	public void shutdown()
	{
		this.started.set(false);
		WrappedTask task = this.refreshTask;
		this.refreshTask = null;
		if (task != null)
		{
			task.cancel();
		}
		this.unregister();
	}

	private void runRefreshCycle()
	{
		if (!this.cycleRunning.compareAndSet(false, true))
		{
			return;
		}
		try
		{
			ConcurrentMap<String, Integer> target = this.buildingCounts;
			this.currentCounts = target;
			this.buildingCounts = new ConcurrentHashMap<>();

			for (Player player : Bukkit.getOnlinePlayers())
			{
				WorldGuardUtils.getScheduler().runAtEntity(player, task -> this.countPlayerRegions(player, this.buildingCounts));
			}
		}
		catch (Throwable t)
		{
			// Never let a bad cycle kill the repeating task.
			this.plugin.getLogger().warning("[PlaceholderAPI] wgefp refresh cycle failed: "
					+ (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
		}
		finally
		{
			this.cycleRunning.set(false);
		}
	}

	private void countPlayerRegions(Player player, ConcurrentMap<String, Integer> target)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		try
		{
			if (this.plugin.getRegionContainer() == null)
			{
				return;
			}
			com.sk89q.worldedit.util.Location location = BukkitAdapter.adapt(player.getLocation());
			ApplicableRegionSet regions = this.plugin.getRegionContainer().createQuery().getApplicableRegions(location);
			for (ProtectedRegion region : regions)
			{
				target.merge(region.getId().toLowerCase(Locale.ROOT), 1, Integer::sum);
			}
		}
		catch (Throwable ignored)
		{
			// Player may have logged off mid-cycle; the region container may have been reloaded.
		}
	}
}
