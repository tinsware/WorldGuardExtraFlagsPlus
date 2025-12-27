# WorldGuard ExtraFlags Plus

An advanced WorldGuard extension that adds over 30+ extra region flags for full control of player behavior, teleportation, and region rules — featuring Folia support, item blocking (Mace, Firework, Wind Charge, Totem), and fully customizable messages.

> 🧱 **Folia Ready** | ⚙️ **Custom Messages** | 🪓 **Disable Mace, Totem, Trident & More**

> 🎚️ **XP-Based or PlaceholderAPI (integer output) based region entry limits**

---

## Key Features

- ✅ **Folia support** – fully compatible with async region handling
- 🛡️ **New flag:** `disable-completely` – blocks all usage of specified items *(MACE, FIREWORK_ROCKET, WIND_CHARGE, TOTEM_OF_UNDYING, TRIDENT)*
  - *Note: `permit-completely` is replaced. Please use `disable-completely` instead.*
- 🎚️ **New flags:** `entry-min-level` / `entry-max-level` – restrict entry by **XP level** or **PlaceholderAPI values**
- 💬 **Customizable messages** via `messages-wgefp.yml` (disable, recolor, or use placeholders)
- 🔁 **Message cooldown system** to prevent spam (default 3 seconds)
- 🏪 **New flag:** `villager-trade` – control villager trading in regions
- 🚫 **New flag:** `disable-collision` – disable player collision in regions
  - *Uses Minecraft's native scoreboard teams to control collision. TAB plugin is supported with API integration. May conflict with other plugins that use teams. See (public-documents/disable-collision flag documentation.md) for details.*
- 📝 **PlaceholderAPI Chat Integration** – chat prefix/suffix supports PlaceholderAPI placeholders
- 🧱 **New flags:** `allow-block-place` / `deny-block-place` / `allow-block-break` / `deny-block-break` – fine-grained block placement and breaking control
- 📦 **New flags:** `deny-item-drops` / `deny-item-pickup` – restrict specific items from being dropped or picked up (works when WorldGuard allows drops/pickups)
- 🔨 **New flag:** `permit-workbenches` – block workbench usage (anvil, crafting table, ender chest, etc.) and crafting table crafting in regions
  - *Note: `permit-workbenches CRAFT` now only blocks crafting table (3x3) crafting, not inventory (2x2) crafting. Use `inventory-craft` flag to block inventory crafting.*
- 🎨 **New flag:** `inventory-craft` – block inventory crafting (2x2 grid) in regions
- 🛡️ **Godmode & Fly Flag Enhancement** – The `godmode` and `fly` flags now also disable EssentialsX godmode/fly when entering regions with these flags disabled (EssentialsX integration)

---

## About

WorldGuard protects land by defining regions.
**WorldGuard ExtraFlags Plus** extends it with even more customization — adding powerful flags that modify gameplay, teleportation, commands, and behavior within regions.

---

## Included Flags (30+)

> Here’s a quick overview — all managed with standard WorldGuard flag commands.

```
teleport-on-entry / teleport-on-exit  
command-on-entry / command-on-exit  
console-command-on-entry / console-command-on-exit  
walk-speed / fly-speed  
keep-inventory / keep-exp  
chat-prefix / chat-suffix  
godmode / blocked-effects  
respawn-location / worldedit / give-effects  
fly / play-sounds / frostwalker / nether-portals / glide (elytra-blocker)
chunk-unload / item-durability / join-location
```

**New in Plus:**

```
disable-completely (old usage was: permit-completely)
entry-min-level
entry-max-level
villager-trade
disable-collision
allow-block-place / deny-block-place / allow-block-break / deny-block-break
deny-item-drops / deny-item-pickup
permit-workbenches
inventory-craft
```

---

## Usage

Use the WorldGuard region flag commands as usual —
all ExtraFlagsPlus flags integrate natively.

Example:

```
/rg flag spawn disable-completely MACE
/rg flag spawn disable-completely MACE,FIREWORK_ROCKET

/rg flag spawn disable-completely clear (especially for inherited child regions)

/rg flag dungeon entry-min-level 20 XP
/rg flag dungeon entry-min-level 40 %battlepass_tier%

/rg flag spawn villager-trade deny

/rg flag spawn disable-collision true

/rg flag spawn allow-block-place STONE,COBBLESTONE,GRASS_BLOCK
/rg flag spawn deny-block-place TNT,LAVA_BUCKET
/rg flag spawn allow-block-break STONE,COBBLESTONE
/rg flag spawn deny-block-break BEDROCK,SPAWNER

/rg flag spawn deny-item-drops diamond,emerald,netherite_ingot
/rg flag spawn deny-item-pickup apple,redstone,iron_ingot

/rg flag spawn permit-workbenches ALL
/rg flag spawn permit-workbenches ALL,ender
/rg flag spawn permit-workbenches craft,anvil,ender
/rg flag spawn permit-workbenches clear

/rg flag spawn inventory-craft deny

/rg flag spawn godmode deny (also disables EssentialsX godmode if enabled)

/rg flag spawn chat-prefix "&7[%vault_rank%] "
/rg flag spawn chat-suffix " &7[%player_level%]"
```

---

## Version Compatibility


| Minecraft       | WorldGuard | ExtraFlagsPlus | Support   |
| --------------- | ---------- | -------------- | --------- |
| 1.20 – 1.21.11 | 7.0.15+    | 4.3.10+         | ✅ Active |
| 1.7 – 1.19     | Older      | ❌ No support  |           |

---

## Message Customization

All plugin messages live in `plugins/WorldGuard/messages-wgefp.yml`.

- Edit freely to match your style
- Use `{required}`, `{current}`, `{item}`, `{workbench}` placeholders
- Color codes supported (`&c`, `&7`, etc.)
- Disable messages with `""`
- Reload instantly using `/wgefp reload` or `/wg reload`

---

## Authors

- **ExtraFlags Plus Developer:** [tins](https://github.com/tins-dev)
- **Original ExtraFlags Author:** [isokissa3](https://joniaromaa.fi)

---

## Support & Community

- 💬 **Discord:** [Join our Discord server](https://discord.gg/TCJAwsdqum)

---

## Image Section

---

⭐ If you like this project, give it a star on [Github](https://github.com/tins-dev/WorldGuardExtraFlagsPlus)
