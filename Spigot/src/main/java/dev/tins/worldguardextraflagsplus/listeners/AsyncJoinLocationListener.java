package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import lombok.RequiredArgsConstructor;

/**
 * Paper-specific listener for join-location flag functionality.
 * Uses AsyncPlayerSpawnLocationEvent instead of the deprecated PlayerSpawnLocationEvent.
 * Only registered on Paper servers when join-location.enabled is true in config.
 */
@RequiredArgsConstructor
public class AsyncJoinLocationListener implements Listener
{
	private final RegionContainer regionContainer;

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAsyncPlayerSpawnLocation(AsyncPlayerSpawnLocationEvent event)
	{
		// null is passed as RegionAssociable - LocationFlag does not depend on region membership,
		// so the flag value is returned regardless of who the player is.
		Location location = this.regionContainer.createQuery().queryValue(
				BukkitAdapter.adapt(event.getSpawnLocation()),
				null,
				dev.tins.worldguardextraflagsplus.flags.Flags.JOIN_LOCATION
		);

		if (location != null)
		{
			event.setSpawnLocation(BukkitAdapter.adapt(location));
		}
	}
}
