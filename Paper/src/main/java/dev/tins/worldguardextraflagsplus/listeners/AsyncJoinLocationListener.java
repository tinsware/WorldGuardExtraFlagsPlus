package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;

import lombok.RequiredArgsConstructor;

/**
 * Paper/Folia listener for {@code join-location}. Uses {@link AsyncPlayerSpawnLocationEvent}
 * instead of deprecated {@link org.spigotmc.event.player.PlayerSpawnLocationEvent}.
 */
@RequiredArgsConstructor
public final class AsyncJoinLocationListener implements Listener
{
	private final RegionContainer regionContainer;

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onAsyncPlayerSpawnLocation(AsyncPlayerSpawnLocationEvent event)
	{
		// No RegionAssociable: player is not fully joined yet. JOIN_LOCATION is a LocationFlag at the
		// candidate spawn position; resolution matches PR #12 / tinsware fork behavior.
		Location location = this.regionContainer.createQuery().queryValue(
				BukkitAdapter.adapt(event.getSpawnLocation()),
				null,
				dev.tins.worldguardextraflagsplus.flags.Flags.JOIN_LOCATION);

		if (location != null)
		{
			event.setSpawnLocation(BukkitAdapter.adapt(location));
		}
	}
}
