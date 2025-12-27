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
 * Dedicated listener for join-location flag functionality.
 * Only registered when join-location.enabled is true in config.
 * This avoids the deprecation warning when the flag is disabled.
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
	 * Although deprecated on Paper, it's still functional and necessary for the join-location flag.
	 * AsyncPlayerSpawnLocationEvent cannot be used because:
	 * 1. It occurs during configuration phase when the player object doesn't exist yet
	 * 2. WorldGuard's ApplicableRegionSet.queryValue() requires a RegionAssociable (LocalPlayer) parameter
	 * 3. Without player context, region permission checks cannot be performed
	 *
	 * The deprecation warning on Paper is unavoidable but the functionality remains correct.
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
