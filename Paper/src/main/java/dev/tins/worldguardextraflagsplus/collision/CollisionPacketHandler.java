package dev.tins.worldguardextraflagsplus.collision;

import org.bukkit.entity.Player;

/**
 * Interface for collision manipulation.
 * Implementations handle disabling/enabling player collision using Minecraft's native systems.
 */
public interface CollisionPacketHandler
{
	/**
	 * Initialize the collision handler (setup teams, etc.)
	 * @return true if initialization succeeded, false otherwise
	 */
	boolean initialize();
	
	/**
	 * Disable collision for a player
	 * @param player The player to disable collision for
	 */
	void disableCollision(Player player);
	
	/**
	 * Enable collision for a player (restore normal collision)
	 * @param player The player to restore collision for
	 */
	void enableCollision(Player player);
	
	/**
	 * Cleanup resources when plugin disables
	 */
	void cleanup();
	
	/**
	 * Get the handler name (for logging/debugging)
	 * @return Handler name (e.g., "Native Teams")
	 */
	String getLibraryName();
}

