# WorldGuard ExtraFlags Plus (WGEFP)

**Release 4.4.6**

An advanced WorldGuard extension that adds **45+ extra region flags** for full control of player behavior, teleportation, and region rules — featuring Folia support, item blocking (Mace, Firework, Wind Charge, Totem, Goat Horn, Spears), throwable-only blocking (`disable-throw` for egg, snowball, pearl, XP bottle), optional PacketEvents/ProtocolLib packet hooks for full `disable-completely` coverage, and fully customizable messages.

## ⚠️ Warning DO NOT USE BOTH PLUGINS TOGETHER!

> If you're upgrading from `WorldGuardExtraFlags` to `WorldGuardExtraFlagsPlus`, make sure to:
> - **Remove** the old `WorldGuardExtraFlags.jar` plugin file
> - **Only keep** `WorldGuardExtraFlagsPlus.jar` on your server
> - Both plugins cannot coexist - they will conflict with each other

## 📖 Full Flag Usage Guide

For comprehensive usage examples and detailed flag documentation, see the **[Complete Flag Usage Guide](https://tinsware.github.io/wiki/docs/games/minecraft/plugins/worldguard-extraflags-plus/flag-usage)** on the tinsware Wiki.

## Why WorldGuard Extra Flags Plus?

- 🪓 **Item & Throwable Control** — `disable-completely` (Mace, Firework, Wind Charge, Totem, Trident, Goat Horn, Spears), `disable-throw` (egg / snowball / pearl / XP bottle), `deny-item-drops` / `deny-item-pickup`
- 🚪 **Entry Control** — XP or PlaceholderAPI level gates (`entry-min-level` / `entry-max-level`), permission gates (`entry-permission` / `entry-deny-permission`), `player-count-limit`
- ⚙️ **Region Automation** — `command-on-entry/exit`, `console-command-on-entry/exit`, repeating console commands (`console-command-repeat`, `console-command-timer`), chat prefix / suffix with PlaceholderAPI; command flags support `%uuid%`
- 🧱 **Block & World Interaction** — block place/break control (incl. buckets & waterlogged blocks), `enderchest-access`, `permit-workbenches`, `inventory-craft`, `villager-trade`, `disable-collision`, `chunk-unload`, `nether-portals`, `worldedit`, `lightning-damage`, `frostwalker`, `play-sounds`
- 🧟 **Mob & Spawn Control** — `deny-mobspawn` by category or entity type
- 🛡️ **Player Protection & Effects** — `godmode`, `keep-inventory` (incl. DeluxeCombat), `keep-exp`, `item-durability`, `fly` / `glide` / speed flags, `blocked-effects` / `give-effects`
- ✨ **Extras** — Folia support, Paper 1.21 – 26.2+, customizable messages + cooldown, update checker, `hide-players` & `chambered-enderpearl` (experimental)

## Flag List (45+)

All managed with standard WorldGuard flag commands.

```
[teleport-on-entry / teleport-on-exit]  [command-on-entry / command-on-exit]
[console-command-on-entry / console-command-on-exit]  [console-command-repeat / console-command-timer]
[walk-speed / fly-speed]  [fly]  [glide](elytra-blocker)  [frostwalker]
[keep-inventory / keep-exp]  [godmode]  [item-durability]
[chat-prefix / chat-suffix]  [blocked-effects]  [give-effects]  [play-sounds]
[respawn-location]  [join-location](not-on-folia)  [worldedit]
[nether-portals]  [chunk-unload]  [villager-trade]  [inventory-craft]
[allow-block-place / deny-block-place]  [allow-block-break / deny-block-break]
[deny-item-drops / deny-item-pickup]  [permit-workbenches]  [enderchest-access]
[disable-completely]  [disable-throw]  [lightning-damage]  [disable-collision]
[entry-min-level / entry-max-level]  [entry-permission / entry-deny-permission]
[deny-mobspawn]  [player-count-limit]  [hide-players](experimental)  [chambered-enderpearl](experimental)
```

## Version Compatibility

| Minecraft       | WorldGuard | ExtraFlagsPlus | Support   |
| --------------- | ---------- | -------------- | --------- |
| 1.21 – 26.2+ | 7.0.15+    | 4.4.6+         | ✅ Active |
| 1.7 – 1.19     | Older      | ❌ No support  |           |

The jar declares `api-version: 1.21` in `plugin.yml` so Paper **1.21.x** servers (and forks such as Canvas) load it.

## Message Customization

All plugin messages live in `plugins/WorldGuard/messages-wgefp.yml`.

- Edit freely to match your style; color codes supported (`&c`, `&7`, etc.); disable messages with `""`
- Placeholders: `{permission}`, `{required}`, `{current}`, `{item}`, `{workbench}` (e.g. `disable-throw-blocked` uses `{item}`)
- Message cooldown (default 2 seconds) prevents spam; reload instantly with `/wgefp reload` or `/wg reload`

## Authors

- **Active Author & Developer of WGEFP:** [tinsware](https://tinsware.github.io/)
- **Original author:** isokissa3 — [https://joniaromaa.fi](https://joniaromaa.fi)

## Support & Community

- 📜 **Release notes:** [GitHub Releases](https://github.com/tinsware/WorldGuardExtraFlagsPlus/releases) (latest: **4.4.6**)
- 💬 **Discord:** [Join our Discord server](https://tinsware.github.io/discord)

⭐ If you like this project, give it a star on [GitHub](https://github.com/tinsware/WorldGuardExtraFlagsPlus)
