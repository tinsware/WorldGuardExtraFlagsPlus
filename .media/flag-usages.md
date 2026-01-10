# WorldGuard ExtraFlags Plus - Flag Usage Examples

## Usage

Use the WorldGuard region flag commands as usual — all ExtraFlagsPlus flags integrate natively.

### Example Commands

#### Item Blocking
```bash
/rg flag spawn disable-completely MACE
/rg flag spawn disable-completely MACE,FIREWORK_ROCKET
/rg flag spawn disable-completely clear (especially for inherited child regions)
```

#### Level Restrictions
```bash
/rg flag dungeon entry-min-level 20 XP
/rg flag dungeon entry-min-level 40 %battlepass_tier%
```

#### Trading & Interaction Control
```bash
/rg flag spawn villager-trade deny
```

#### Player Count Limits
```bash
/rg flag arena player-count-limit 10
```

#### Collision Control
```bash
/rg flag spawn disable-collision true
```

#### Block Control
```bash
/rg flag spawn allow-block-place STONE,COBBLESTONE,GRASS_BLOCK
/rg flag spawn deny-block-place TNT,LAVA_BUCKET
/rg flag spawn allow-block-break STONE,COBBLESTONE
/rg flag spawn deny-block-break BEDROCK,SPAWNER
```

#### Item Control
```bash
/rg flag spawn deny-item-drops diamond,emerald,netherite_ingot
/rg flag spawn deny-item-pickup apple,redstone,iron_ingot
```

#### Workbench Control
```bash
/rg flag spawn permit-workbenches ALL
/rg flag spawn permit-workbenches ALL,ender
/rg flag spawn permit-workbenches craft,anvil,ender
/rg flag spawn permit-workbenches clear
```

#### Crafting Control
```bash
/rg flag spawn inventory-craft deny
```

#### Protection & Movement
```bash
/rg flag spawn godmode deny (also disables EssentialsX godmode if enabled)
```

#### Chat Formatting
```bash
/rg flag spawn chat-prefix "&7[%vault_rank%] "
/rg flag spawn chat-suffix " &7[%player_level%]"
```

## Flag Categories

### Location & Teleportation
- `teleport-on-entry` / `teleport-on-exit`
- `join-location` (not available on Folia)
- `respawn-location`

### Command Execution
- `command-on-entry` / `command-on-exit`
- `console-command-on-entry` / `console-command-on-exit`

### Movement & Speed Control
- `walk-speed` / `fly-speed`
- `fly` / `glide`

### Protection & Survival
- `godmode` / `keep-inventory` / `keep-exp`
- `item-durability`

### Chat Modification
- `chat-prefix` / `chat-suffix`

### Effect Control
- `blocked-effects` / `give-effects`

### World Interaction
- `worldedit` / `play-sounds`
- `nether-portals` / `chunk-unload`
- `villager-trade` / `inventory-craft`

### Block & Item Control
- `allow-block-place` / `deny-block-place`
- `allow-block-break` / `deny-block-break`
- `deny-item-drops` / `deny-item-pickup`
- `disable-completely` / `permit-workbenches`

### Entry Control
- `entry-min-level` / `entry-max-level`
- `player-count-limit`

### Special Features
- `disable-collision`

## Notes

- All flags use standard WorldGuard syntax: `/rg flag <region> <flag> <value>`
- Use `clear` to remove flag values: `/rg flag <region> <flag> clear`
- Some flags require specific plugins (PlaceholderAPI for placeholder support)
- Check individual flag documentation for advanced usage
