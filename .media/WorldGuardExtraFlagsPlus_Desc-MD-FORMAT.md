# WorldGuard ExtraFlags Plus

An advanced WorldGuard extension that adds over 30+ extra region flags for full control of player behavior, teleportation, and region rules — featuring Folia support, item blocking (Mace, Firework, Wind Charge, Totem, vanilla Spears), throwable-only blocking (`disable-throw` for egg, snowball, pearl, XP bottle), optional PacketEvents/ProtocolLib packet hooks for full `disable-completely` coverage, and fully customizable messages.

> 🧱 **Folia Ready** | ⚙️ **Custom Messages** | 🪓 **Disable Mace, Totem, Trident, Spears & More** | 🥚 **Disable-throw (egg / snowball / pearl / XP bottle)**

> 🎚️ **XP-Based or PlaceholderAPI (integer output) based region entry limits**

---

## ⚠️ Warning DO NOT USE BOTH PLUGINS TOGETHER!

> If you're upgrading from `WorldGuardExtraFlags` to `WorldGuardExtraFlagsPlus`, make sure to:
> - **Remove** the old `WorldGuardExtraFlags.jar` plugin file
> - **Only keep** `WorldGuardExtraFlagsPlus.jar` on your server
> - Both plugins cannot coexist - they will conflict with each other

---

## Key Features

- ✅ **Folia support** – fully compatible with async region handling
- 🛡️ **New flag:** `disable-completely` – blocks all usage of specified items *(MACE, FIREWORK_ROCKET, WIND_CHARGE, TOTEM_OF_UNDYING, TRIDENT, vanilla spear tiers 1.21.11+, plus **`SPEAR`** for all tiers at once)*
  - *Note: `permit-completely` is replaced. Please use `disable-completely` instead.*
  - *Spear **Lunge** (packet **STAB**, including **Lunge** enchant) requires **PacketEvents** or **ProtocolLib** on the server for reliable blocking; without either, Lunge may bypass region checks.*
- 🎯 **New flag:** `disable-throw` – blocks **launching** only these throwables: **`EGG`**, **`SNOWBALL`**, **`ENDER_PEARL`**, **`EXPERIENCE_BOTTLE`**. Tridents and wind charges stay on `disable-completely`.
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
- 👥 **New flag:** `player-count-limit` – limit maximum number of players in a region
- 🎯 **New flag (experimental):** `chambered-enderpearl` – mitigates chambered ender pearl bypasses (pearls thrown outside denied regions are removed when the shooter enters a region where the flag denies); subject to change.

---

## About

WorldGuard protects land by defining regions.
**WorldGuard ExtraFlags Plus** extends it with even more customization — adding powerful flags that modify gameplay, teleportation, commands, and behavior within regions.

---

## Included Flags (30+)

> Here’s a quick overview — all managed with standard WorldGuard flag commands.

```
[teleport-on-entry / teleport-on-exit]  [command-on-entry / command-on-exit]
[console-command-on-entry / console-command-on-exit]  [walk-speed / fly-speed]
[keep-inventory / keep-exp]  [chat-prefix / chat-suffix]  [godmode] 
[blocked-effects]  [respawn-location]  [worldedit]  [give-effects]  
[fly]  [play-sounds]  [frostwalker]  [nether-portals]  [glide](elytra-blocker)
[chunk-unload]  [item-durability]  [join-location](not-on-folia)
```

**New in Plus:**

```
[disable-completely]  [disable-throw]
[entry-min-level / entry-max-level]
[villager-trade]  [disable-collision]  [deny-item-drops / deny-item-pickup]
[allow-block-place / deny-block-place]  [allow-block-break / deny-block-break]
[permit-workbenches]  [inventory-craft]  [player-count-limit]
[chambered-enderpearl](experimental)
```

---

## 📖 Flag Usage Guide

For comprehensive usage examples and detailed flag documentation, see:
**[Complete Flag Usage Guide](https://github.com/tins-dev/WorldGuardExtraFlagsPlus/blob/master/.media/flag-usages.md)**

---

## Version Compatibility


| Minecraft       | WorldGuard | ExtraFlagsPlus | Support   |
| --------------- | ---------- | -------------- | --------- |
| 1.20 – 1.21.11 | 7.0.15+    | 4.3.12+         | ✅ Active |
| 1.7 – 1.19     | Older      | ❌ No support  |           |

The jar declares **`api-version: 1.21`** in `plugin.yml` so Paper **1.21.x** servers (and forks such as Canvas) load it; the project still compiles against the pinned Paper API artifact in the root `pom.xml`.

---

## Message Customization

All plugin messages live in `plugins/WorldGuard/messages-wgefp.yml`.

- Edit freely to match your style
- Use `{required}`, `{current}`, `{item}`, `{workbench}` placeholders (e.g. **`disable-throw-blocked`** uses `{item}`)
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
