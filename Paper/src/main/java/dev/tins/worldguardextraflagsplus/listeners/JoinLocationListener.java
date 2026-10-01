package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import lombok.RequiredArgsConstructor;

/**
 * Dedicated listener for {@code join-location} on runtimes without Paper's
 * {@link io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent} (e.g. plain Spigot).
 * On Paper/Folia, {@link AsyncJoinLocationListener} is used instead to avoid deprecated
 * {@link PlayerSpawnLocationEvent}.
 */
@RequiredArgsConstructor
public class JoinLocationListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;

	/**
	 * Handles player spawn location for join-location flag functionality.
	 * This event provides access to the player object needed for WorldGuard region queries.
	 *
	 * Deprecated on Paper in favor of {@link io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent}.
	 * This handler remains for Spigot; Paper servers use {@link AsyncJoinLocationListener} instead.
	 */
	@EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
	public void onPlayerSpawnLocationEvent(PlayerSpawnLocationEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);

		Location location = this.regionContainer.createQuery().queryValue(BukkitAdapter.adapt(event.getSpawnLocation()), localPlayer, dev.tins.worldguardextraflagsplus.flags.Flags.JOIN_LOCATION);
		if (location != null)
		{
			event.setSpawnLocation(BukkitAdapter.adapt(location));
		}
	}
}
