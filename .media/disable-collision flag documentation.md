# Disable Collision Flag - Complete Documentation

## Overview

The `disable-collision` flag allows you to disable player-to-player collision within specific WorldGuard regions. 
This is useful for creating areas where players can pass through each other.

## How It Works

The `disable-collision` flag uses **Minecraft's native scoreboard team system** to control player collision. 
This is the only method that Minecraft provides for controlling entity-to-entity collision, and it's the most reliable and compatible approach.

### Technical Implementation

The plugin uses a **hybrid team-based approach** that handles conflicts with other plugins:

1. **Single-Player Teams**: If a player is the only member of their team, the plugin temporarily modifies that team's collision rule to `NEVER` (disables collision). When the player leaves the region, the original collision rule is restored.

2. **Per-Player Teams**: If a player is in a team with multiple members, the plugin creates a temporary per-player team (named `WGC_<hash>`) and moves the player to it. This prevents affecting other team members. When the player leaves the region, they are restored to their original team.

3. **No Team**: If a player is not in any team, they are added to a per-player team with collision disabled. When they leave the region, they are removed from the team.


### Examples Usages

**Disable collision in spawn area:**
```
/rg flag spawn disable-collision true
```

**Remove collision disabling:**
```
/rg flag <region> disable-collision false
```

## Important Considerations

### Team Conflicts

**The Good News:**
- The plugin is designed to minimize conflicts with other plugins that use teams
- It handles players already in teams
- It restores original team settings when players leave regions

**Potential Edge Cases:**

1. **Shared Teams with Multiple Members:**
   - If Player A and Player B are both in "TeamX" and Player A enters a collision-disabled region, the plugin will create a per-player team for Player A
   - Player B remains in TeamX and is unaffected
   - This prevents unintended collision changes for other team members

2. **Team Modifications by Other Plugins:**
   - If another plugin modifies a team's collision rule while a player is in a collision-disabled region, the plugin will restore the rule it stored when the player leaves
   - This means the other plugin's changes may be overwritten when the player exits the region
   - **Recommendation**: Coordinate team usage with other plugins, or ensure players in collision-disabled regions are not in shared teams

3. **Team Removal:**
   - If a team is deleted by another plugin while a player is in a collision-disabled region, the plugin will attempt to restore the player to that team when they leave
   - This will fail gracefully, and the player will simply not be in a team after leaving

4. **TAB Integration:**
   - If TAB plugin is installed, the plugin uses TAB's API to set collision rules
   - This prevents TAB from overwriting collision settings
   - TAB integration takes priority over direct team manipulation
   - If TAB is not available, the plugin falls back to direct team manipulation


### Compatibility

**Plugins:**
- ✅ Compatible with most plugins
- ✅ **TAB Plugin**: Fully supported with API integration. If TAB is installed, the plugin uses TAB's API to manage collision, preventing conflicts.
- ⚠️ May conflict with plugins that heavily modify team collision rules (CMI, NametagEdit, DeluxeTags, etc.)
- ⚠️ Coordinate with plugins that use teams for player grouping (e.g., party systems, guild systems)

**Note:** When the plugin loads, it will display a warning if plugins that use teams are detected. The plugin will still function, but conflicts may occur. TAB is the only plugin with official API integration support.


## Troubleshooting

### Players Still Colliding

1. **Check the flag is set correctly:**
   ```
   /rg flags <region>
   ```
   Look for `disable-collision: true`

2. **Check if the player is in the region:**
   ```
   /rg info <region>
   ```
   Verify the player's location is within the region boundaries

3. **Check server logs:**
   Look for `[Collision]` or `[Collision Flag]` warning messages in your server console/logs
   - If you see errors, the collision system may not be initialized properly
   - Check if scoreboard is available and TAB integration is working (if TAB is installed)

4. **Reload the plugin:**
   ```
   /wgefp reload
   ```
   or
   ```
   /wg reload
   ```

### Team Conflicts

If you experience issues with other plugins that use teams:

1. **Check which plugins use teams:**
   - TAB (supported with API integration)
   - CMI, NametagEdit, DeluxeTags, VentureChat, LuckPerms (may conflict)
   - Party plugins
   - Guild/clan plugins
   - PvP arena plugins
   - Other protection plugins

2. **Test in isolation:**
   - Temporarily disable other plugins that use teams
   - Test if collision disabling works
   - Re-enable plugins one by one to identify conflicts

### Players Getting Kicked

If players are getting kicked when entering collision-disabled regions:

1. **Check for team-related errors in logs:**
   - Look for `IllegalStateException` or team-related errors
   - This usually indicates a conflict with another plugin

2. **Check team limits:**
   - Minecraft has a limit on team names (16 characters)
   - The plugin uses `WGC_<hash>` format (4 chars prefix + 8 char hex hash = 12 chars total, well within the 16 character limit)

3. **Check scoreboard availability:**
   - Ensure the server's scoreboard system is functioning
   - Try restarting the server if scoreboard seems corrupted

## Technical Details

### Team Name Format

Per-player teams are named: `WGC_<hash>`
- `WGC_` = WorldGuard Collision prefix (4 characters)
- `<hash>` = Hexadecimal representation of the player's UUID hash code (8 characters)
- **Total length: 12 characters** (well within Minecraft's 16 character limit)

Example: `WGC_a1b2c3d4`

**Note:** The hash is derived from the player's UUID using Java's `hashCode()` method, ensuring uniqueness for practical purposes while keeping the team name short.

---


