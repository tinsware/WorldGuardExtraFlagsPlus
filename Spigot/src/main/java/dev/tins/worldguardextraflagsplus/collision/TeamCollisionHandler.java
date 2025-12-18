package dev.tins.worldguardextraflagsplus.collision;

import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * Team-based collision handler.
 * Uses Minecraft's native scoreboard teams to disable player collision.
 * Handles conflicts with other plugins by modifying existing team's collision rule when possible,
 * or creating per-player teams when necessary.
 */
public class TeamCollisionHandler implements CollisionPacketHandler
{
	private static final String TEAM_NAME_PREFIX = "WGC_"; // WGC = WorldGuard Collision, 4 chars
	
	private final WorldGuardExtraFlagsPlusPlugin plugin;
	private final Set<UUID> collisionDisabledPlayers = Collections.synchronizedSet(new HashSet<>());
	// Store original team collision rule for players already in teams (when we modify their team)
	private final Map<UUID, Team.OptionStatus> originalCollisionRules = new HashMap<>();
	// Store which team we modified (for restoration)
	private final Map<UUID, Team> modifiedTeams = new HashMap<>();
	// Store per-player teams we created (for cleanup)
	private final Map<UUID, Team> perPlayerTeams = new HashMap<>();
	
	public TeamCollisionHandler(WorldGuardExtraFlagsPlusPlugin plugin)
	{
		this.plugin = plugin;
	}
	
	@Override
	public boolean initialize()
	{
		try
		{
			// Try to initialize TAB integration first
			if (TabIntegration.initialize())
			{
				plugin.getLogger().info("[Collision] TAB integration initialized - using TAB API for collision management");
				return true;
			}
			
			// Fall back to direct team manipulation if TAB is not available
			// Verify scoreboard is available
			ScoreboardManager manager = plugin.getServer().getScoreboardManager();
			if (manager == null)
			{
				plugin.getLogger().warning("[Collision] ScoreboardManager is null!");
				return false;
			}
			
			Scoreboard scoreboard = manager.getMainScoreboard();
			if (scoreboard == null)
			{
				plugin.getLogger().warning("[Collision] Main scoreboard is null!");
				return false;
			}
			
			return true;
		}
		catch (Exception e)
		{
			plugin.getLogger().warning("[Collision] Failed to initialize collision handler: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public void disableCollision(Player player)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		// Use TAB API if available (prevents TAB from overwriting our changes)
		if (TabIntegration.isAvailable())
		{
			if (TabIntegration.setCollisionRule(player, true))
			{
				collisionDisabledPlayers.add(player.getUniqueId());
				return;
			}
			// If TAB API call failed, fall through to direct team manipulation
		}
		
		try
		{
			Scoreboard scoreboard = player.getScoreboard();
			Team currentTeam = scoreboard.getPlayerTeam(player);
			
			if (currentTeam != null)
			{
				// Player is already in a team
				int teamSize = currentTeam.getEntries().size();
				
				if (teamSize == 1)
				{
					// Player is the only member - safe to modify team's collision rule
					Team.OptionStatus currentRule = currentTeam.getOption(Team.Option.COLLISION_RULE);
					originalCollisionRules.put(player.getUniqueId(), currentRule);
					modifiedTeams.put(player.getUniqueId(), currentTeam);
					currentTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
					collisionDisabledPlayers.add(player.getUniqueId());
				}
				else
				{
					// Multiple players in team - create per-player team to avoid affecting others
					// Use hash of UUID to keep team name within 16 character limit
					String perPlayerTeamName = TEAM_NAME_PREFIX + Integer.toHexString(player.getUniqueId().hashCode());
					Team perPlayerTeam = scoreboard.getTeam(perPlayerTeamName);
					
					if (perPlayerTeam == null)
					{
						perPlayerTeam = scoreboard.registerNewTeam(perPlayerTeamName);
						perPlayerTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
					}
					
					// Remove from current team and add to per-player team
					currentTeam.removeEntry(player.getName());
					perPlayerTeam.addEntry(player.getName());
					perPlayerTeams.put(player.getUniqueId(), perPlayerTeam);
					modifiedTeams.put(player.getUniqueId(), currentTeam);
					collisionDisabledPlayers.add(player.getUniqueId());
				}
			}
			else
			{
				// Player is not in any team - create per-player team
				// Use hash of UUID to keep team name within 16 character limit
				String perPlayerTeamName = TEAM_NAME_PREFIX + Integer.toHexString(player.getUniqueId().hashCode());
				Team perPlayerTeam = scoreboard.getTeam(perPlayerTeamName);
				
				if (perPlayerTeam == null)
				{
					perPlayerTeam = scoreboard.registerNewTeam(perPlayerTeamName);
					perPlayerTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
				}
				
				perPlayerTeam.addEntry(player.getName());
				perPlayerTeams.put(player.getUniqueId(), perPlayerTeam);
				collisionDisabledPlayers.add(player.getUniqueId());
			}
		}
		catch (Exception e)
		{
			plugin.getLogger().warning("[Collision] Failed to disable collision for " + player.getName() + ": " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			e.printStackTrace();
		}
	}
	
	@Override
	public void enableCollision(Player player)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		UUID playerUUID = player.getUniqueId();
		
		// Use TAB API if available (prevents TAB from overwriting our changes)
		if (TabIntegration.isAvailable())
		{
			// Set to null to remove forced collision value and let TAB manage it normally
			if (TabIntegration.setCollisionRule(player, null))
			{
				collisionDisabledPlayers.remove(playerUUID);
				return;
			}
			// If TAB API call failed, fall through to direct team manipulation
		}
		
		try
		{
			Scoreboard scoreboard = player.getScoreboard();
			
			// First, handle players we tracked
			if (perPlayerTeams.containsKey(playerUUID))
			{
				// Player was in a per-player team - remove them and restore to original team
				Team perPlayerTeam = perPlayerTeams.remove(playerUUID);
				Team originalTeam = modifiedTeams.remove(playerUUID);
				
				// Remove player from per-player team
				perPlayerTeam.removeEntry(player.getName());
				
				// Restore to original team if they had one
				if (originalTeam != null)
				{
					originalTeam.addEntry(player.getName());
				}
				// If they had no original team, they're now team-less which means normal collision
			}
			else if (modifiedTeams.containsKey(playerUUID))
			{
				// Player's team collision rule was modified - restore it
				Team team = modifiedTeams.remove(playerUUID);
				Team.OptionStatus originalRule = originalCollisionRules.remove(playerUUID);
				
				if (team != null && originalRule != null)
				{
					team.setOption(Team.Option.COLLISION_RULE, originalRule);
				}
			}
			
			// Final safety check: ensure player's current team has proper collision rule
			// This handles cases where the player might still be in a collision-disabled team
			// Re-fetch current team after any modifications above
			Team currentTeam = scoreboard.getPlayerTeam(player);
			if (currentTeam != null && collisionDisabledPlayers.contains(playerUUID))
			{
				String teamName = currentTeam.getName();
				if (teamName.startsWith(TEAM_NAME_PREFIX))
				{
					// Player is still in a per-player team - remove them
					currentTeam.removeEntry(player.getName());
				}
				else if (currentTeam.getOption(Team.Option.COLLISION_RULE) == Team.OptionStatus.NEVER)
				{
					// Player's team has collision disabled - restore to ALWAYS (default)
					currentTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
				}
			}
			
			collisionDisabledPlayers.remove(playerUUID);
		}
		catch (Exception e)
		{
			plugin.getLogger().warning("[Collision] Failed to enable collision for " + player.getName() + ": " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			e.printStackTrace();
		}
	}
	
	@Override
	public void cleanup()
	{
		// Restore all modified teams
		for (Map.Entry<UUID, Team> entry : modifiedTeams.entrySet())
		{
			try
			{
				Team team = entry.getValue();
				Team.OptionStatus originalRule = originalCollisionRules.get(entry.getKey());
				if (team != null && originalRule != null)
				{
					team.setOption(Team.Option.COLLISION_RULE, originalRule);
				}
			}
			catch (Exception e)
			{
				// Ignore cleanup errors
			}
		}
		
		// Remove all players from per-player teams and unregister teams
		for (Map.Entry<UUID, Team> entry : perPlayerTeams.entrySet())
		{
			try
			{
				Team team = entry.getValue();
				Set<String> entries = new HashSet<>(team.getEntries());
				for (String teamEntry : entries)
				{
					team.removeEntry(teamEntry);
				}
				team.unregister();
			}
			catch (Exception e)
			{
				// Ignore cleanup errors
			}
		}
		
		collisionDisabledPlayers.clear();
		originalCollisionRules.clear();
		modifiedTeams.clear();
		perPlayerTeams.clear();
	}
	
	@Override
	public String getLibraryName()
	{
		return "Native Teams";
	}
}

