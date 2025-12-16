package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class CollisionFlagHandler extends FlagValueChangeHandler<Boolean>
{
	// Unique team name to avoid conflicts with other plugins
	// Using plugin prefix and descriptive name to minimize collision risk
	private static final String TEAM_NAME = "WGEFP_COLLISION_DISABLED";
	private static volatile boolean teamInitialized = false;
	private static volatile boolean collisionEnabled = false; // Whether collision flag is functional
	private static final Object teamInitLock = new Object();
	
	@Getter
	private Boolean currentValue;
	
	/**
	 * Initialize the collision team during plugin startup.
	 * Tries to create the team on the main scoreboard.
	 * If that fails (Folia), the collision flag will be disabled.
	 * This should be called from the plugin's onEnable() method.
	 */
	public static void initializeTeam()
	{
		synchronized (teamInitLock)
		{
			if (teamInitialized)
			{
				return;
			}
			
			try
			{
				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				if (scoreboard == null)
				{
					Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Main scoreboard is null, collision flag will be disabled.");
					collisionEnabled = false;
					teamInitialized = true;
					return;
				}
				
				// Check if team already exists (might be pre-created)
				Team team = scoreboard.getTeam(TEAM_NAME);
				if (team == null)
				{
					// Try to create it - will fail in Folia but might work in Paper/Spigot
					try
					{
						team = scoreboard.registerNewTeam(TEAM_NAME);
						team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
						collisionEnabled = true;
						Bukkit.getLogger().info("[WorldGuardExtraFlagsPlus] Collision team created on main scoreboard.");
					}
					catch (UnsupportedOperationException e)
					{
						// Folia doesn't support team registration on main scoreboard
						// Disable the collision flag feature on Folia
						collisionEnabled = false;
						Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Main scoreboard team creation not supported (Folia detected).");
						Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] The 'disable-collision' flag is not supported on Folia and will be disabled.");
					}
				}
				else
				{
					// Team already exists, just set the collision rule
					team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
					collisionEnabled = true;
					Bukkit.getLogger().info("[WorldGuardExtraFlagsPlus] Collision team found on main scoreboard.");
				}
				
				teamInitialized = true;
			}
			catch (Exception e)
			{
				Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to initialize collision team: " + 
					(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
				collisionEnabled = false;
				teamInitialized = true;
			}
		}
	}
	
	/**
	 * Check if collision flag is enabled and team is available.
	 * Returns true only if collision feature is functional.
	 */
	private static boolean isCollisionEnabled()
	{
		if (!teamInitialized)
		{
			// Team not initialized yet, try to initialize now
			initializeTeam();
		}
		
		return collisionEnabled;
	}
	
	/**
	 * Get the collision team from main scoreboard.
	 * Returns null if not available.
	 */
	private static Team getCollisionTeam()
	{
		if (!isCollisionEnabled())
		{
			return null;
		}
		
		try
		{
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			if (scoreboard == null)
			{
				return null;
			}
			
			Team team = scoreboard.getTeam(TEAM_NAME);
			if (team != null)
			{
				// Ensure collision rule is set
				team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			}
			
			return team;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
	public static class Factory extends Handler.Factory<CollisionFlagHandler>
	{
		@Override
		public CollisionFlagHandler create(Session session)
		{
			return new CollisionFlagHandler(session);
		}
	}
	
	protected CollisionFlagHandler(Session session)
	{
		super(session, Flags.DISABLE_COLLISION);
	}
	
	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Boolean value)
	{
		this.handleValue(player, player.getWorld(), value);
	}
	
	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Boolean currentValue, Boolean lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}
	
	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Boolean lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}
	
	private void handleValue(LocalPlayer player, World world, Boolean disableCollision)
	{
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();
		
		// Don't schedule tasks during shutdown
		if (!WorldGuardUtils.isPluginEnabled() || !bukkitPlayer.isOnline())
		{
			return;
		}
		
		// Check if player has bypass
		if (this.getSession().getManager().hasBypass(player, world))
		{
			// Remove from collision team if has bypass
			if (bukkitPlayer == null || !bukkitPlayer.isOnline())
			{
				return;
			}
			String playerName = bukkitPlayer.getName();
			java.util.UUID playerUUID = bukkitPlayer.getUniqueId();
			
			// Scoreboard operations need entity thread in Folia
			WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
				Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUUID);
				if (onlinePlayer != null && onlinePlayer.isOnline())
				{
					removeFromCollisionTeam(onlinePlayer, playerName);
				}
			});
			return;
		}
		
		// Store current value
		this.currentValue = disableCollision;
		
		// Capture player name before scheduling (important for Folia thread safety)
		if (bukkitPlayer == null || !bukkitPlayer.isOnline())
		{
			return;
		}
		String playerName = bukkitPlayer.getName();
		java.util.UUID playerUUID = bukkitPlayer.getUniqueId();
		
		// Check if collision feature is enabled
		if (!isCollisionEnabled())
		{
			// Collision flag is not supported (e.g., on Folia)
			return;
		}
		
		// In Folia, we need to run on entity thread to access scoreboard
		WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
			// Get player by UUID (safer than using the object directly in Folia)
			Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUUID);
			if (onlinePlayer == null || !onlinePlayer.isOnline())
			{
				return;
			}
			
			if (disableCollision != null && disableCollision)
			{
				// Add to collision-disabled team
				addToCollisionTeam(onlinePlayer, playerName);
			}
			else
			{
				// Remove from collision-disabled team
				removeFromCollisionTeam(onlinePlayer, playerName);
			}
		});
	}
	
	/**
	 * Adds player to collision-disabled team.
	 * Only works if collision feature is enabled (team exists on main scoreboard).
	 * Handles cases where player might be on a different team.
	 */
	private void addToCollisionTeam(Player player, String playerName)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		if (playerName == null || playerName.isEmpty())
		{
			return;
		}
		
		Team team = getCollisionTeam();
		if (team == null)
		{
			// Team not available, collision feature is disabled
			return;
		}
		
		try
		{
			// Check if player is already on this team
			if (team.hasEntry(playerName))
			{
				// Player is already on the team, just ensure collision rule is set
				team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
				return;
			}
			
			// Always re-apply collision rule to ensure it's correct
			// (defensive: in case another plugin modified it)
			team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			
			// Add player to team
			team.addEntry(playerName);
		}
		catch (IllegalStateException e)
		{
			// Player might be on another team - this is okay, we'll try to handle it
			// Check if they're already on our team (race condition)
			try
			{
				if (team.hasEntry(playerName))
				{
					// They're already on our team, just ensure collision rule is set
					team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
				}
				else
				{
					// Player is on a different team - log but don't fail
					// The collision might not work for this player, but we won't crash
					org.bukkit.Bukkit.getLogger().fine("[WorldGuardExtraFlagsPlus] Player " + playerName + 
						" is on a different team, cannot add to collision team.");
				}
			}
			catch (Exception e2)
			{
				// Ignore secondary errors
			}
		}
		catch (Exception e)
		{
			// Log error for debugging
			org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to add player to collision team: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
		}
	}
	
	/**
	 * Public method to manually apply collision settings
	 * Useful for ensuring collision is applied on player join
	 */
	public void applyCollisionSetting(Player player, Boolean disableCollision)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		String playerName = player.getName();
		
		if (disableCollision != null && disableCollision)
		{
			addToCollisionTeam(player, playerName);
		}
		else
		{
			removeFromCollisionTeam(player, playerName);
		}
	}
	
	/**
	 * Removes player from collision-disabled team.
	 * Only works if collision feature is enabled (team exists on main scoreboard).
	 * Handles cases where player might be on a different team or not on any team.
	 * This method is defensive and will not throw exceptions that could cause client disconnections.
	 */
	private void removeFromCollisionTeam(Player player, String playerName)
	{
		if (player == null)
		{
			return;
		}
		
		if (playerName == null || playerName.isEmpty())
		{
			return;
		}
		
		Team team = getCollisionTeam();
		if (team == null)
		{
			// Team not available, collision feature is disabled
			return;
		}
		
		// Check if player is on this team before trying to remove
		// This prevents sending invalid packets to the client
		try
		{
			if (team.hasEntry(playerName))
			{
				team.removeEntry(playerName);
			}
		}
		catch (IllegalStateException e)
		{
			// Player is either on another team or not on any team
			// This can happen due to race conditions between our check and the removal
			// Silently ignore - the player is not on our team, which is what we want
			// This exception is caught on the server side, preventing it from being sent to the client
		}
		catch (Exception e)
		{
			// Log other errors for debugging (but don't let them propagate)
			org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to remove player from collision team: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
		}
	}
}

