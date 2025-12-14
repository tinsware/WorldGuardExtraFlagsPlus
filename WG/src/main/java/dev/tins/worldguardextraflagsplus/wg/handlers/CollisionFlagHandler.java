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
	private static final Object teamInitLock = new Object();
	
	@Getter
	private Boolean currentValue;
	
	/**
	 * Ensure the collision team exists on the main scoreboard.
	 * In Folia, team registration on main scoreboard is not supported.
	 * This method checks if the team exists (possibly pre-created) and sets it up.
	 * This method is thread-safe and will only log warnings once.
	 */
	private static boolean ensureTeamInitialized()
	{
		if (teamInitialized)
		{
			return true;
		}
		
		synchronized (teamInitLock)
		{
			// Double-check after acquiring lock
			if (teamInitialized)
			{
				return true;
			}
			
			try
			{
				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				if (scoreboard == null)
				{
					return false;
				}
				
				// Check if team already exists (might be pre-created)
				Team team = scoreboard.getTeam(TEAM_NAME);
				if (team == null)
				{
					// Try to create it - will fail in Folia but might work in Paper/Spigot
					try
					{
						team = scoreboard.registerNewTeam(TEAM_NAME);
					}
					catch (UnsupportedOperationException e)
					{
						// Folia doesn't support team registration on main scoreboard
						// The team must be pre-created manually or by another plugin
						if (!teamInitialized) // Only log once
						{
							Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Folia does not support registering teams on main scoreboard.");
							Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] The 'disable-collision' flag requires team '" + TEAM_NAME + "' to be pre-created.");
							Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Collision flag will not work until the team is created manually.");
						}
						teamInitialized = true; // Mark as "checked" to prevent spam
						return false;
					}
				}
				
				// Set collision rule
				team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
				teamInitialized = true;
				return true;
			}
			catch (Exception e)
			{
				Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to initialize collision team: " + 
					(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
				return false;
			}
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
					if (ensureTeamInitialized())
					{
						removeFromCollisionTeam(onlinePlayer, playerName);
					}
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
		
		// In Folia, we need to run on entity thread to access scoreboard
		// Try entity thread first, fallback to global if needed
		WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
			// Get player by UUID (safer than using the object directly in Folia)
			Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUUID);
			if (onlinePlayer == null || !onlinePlayer.isOnline())
			{
				return;
			}
			
			// Ensure team is initialized (thread-safe)
			if (!ensureTeamInitialized())
			{
				// Team doesn't exist and couldn't be created (Folia limitation)
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
	 * Adds player to collision-disabled team
	 * Uses the main server scoreboard so all players share the same team.
	 * Note: Main scoreboard is shared across all plugins, but our unique team name
	 * minimizes conflicts. We re-apply collision rule each time to ensure it's correct
	 * even if another plugin modifies the team.
	 */
	private void addToCollisionTeam(Player player, String playerName)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		try
		{
			// Use main server scoreboard so all players share the same team
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			if (scoreboard == null)
			{
				org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Main scoreboard is null!");
				return;
			}
			
			// Get team (should already exist from initialization)
			Team team = scoreboard.getTeam(TEAM_NAME);
			if (team == null)
			{
				// Team doesn't exist - try to create it
				// This might fail in Folia, but we'll try
				try
				{
					team = scoreboard.registerNewTeam(TEAM_NAME);
					team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
				}
				catch (UnsupportedOperationException e)
				{
					// Folia doesn't support team registration on main scoreboard
					// Collision flag won't work in this case
					return;
				}
				catch (Exception e)
				{
					return;
				}
			}
			
			// Always re-apply collision rule to ensure it's correct
			// (defensive: in case another plugin modified it)
			team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			
			// Add player to team if not already added
			// Use provided player name (captured before scheduling for thread safety)
			if (playerName == null || playerName.isEmpty())
			{
				org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Player name is null or empty!");
				return;
			}
			
			if (!team.hasEntry(playerName))
			{
				team.addEntry(playerName);
			}
		}
		catch (IllegalStateException e)
		{
			// Team name might already exist from another plugin - try to use existing team
			try
			{
				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				if (scoreboard == null)
				{
					return;
				}
				
				Team team = scoreboard.getTeam(TEAM_NAME);
				if (team != null && playerName != null && !playerName.isEmpty())
				{
					team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
					if (!team.hasEntry(playerName))
					{
						team.addEntry(playerName);
					}
				}
			}
			catch (Exception e2)
			{
				// Log error for debugging with full exception details
				org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to add player to collision team (retry): " + 
					(e2.getMessage() != null ? e2.getMessage() : e2.getClass().getSimpleName()));
				e2.printStackTrace();
			}
		}
		catch (Exception e)
		{
			// Log error for debugging with full exception details
			org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to add player to collision team: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			e.printStackTrace();
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
	 * Removes player from collision-disabled team
	 * Uses the main server scoreboard
	 */
	private void removeFromCollisionTeam(Player player, String playerName)
	{
		if (player == null)
		{
			return;
		}
		
		try
		{
			// Use main server scoreboard
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			if (scoreboard == null)
			{
				return;
			}
			
			Team team = scoreboard.getTeam(TEAM_NAME);
			if (team != null && playerName != null && !playerName.isEmpty() && team.hasEntry(playerName))
			{
				team.removeEntry(playerName);
			}
		}
		catch (Exception e)
		{
			// Log error for debugging with full exception details
			org.bukkit.Bukkit.getLogger().warning("[WorldGuardExtraFlagsPlus] Failed to remove player from collision team: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			e.printStackTrace();
		}
	}
}

