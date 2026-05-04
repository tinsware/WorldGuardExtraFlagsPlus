# WorldGuard ExtraFlags Plus

A Bukkit plugin extension that provides extra flags for [WorldGuard](https://github.com/EngineHub/WorldGuard).

## ⚠️ Warning DO NOT USE BOTH PLUGINS TOGETHER!

> If you're upgrading from `WorldGuardExtraFlags` to `WorldGuardExtraFlagsPlus`, make sure to:
> - **Remove** the old `WorldGuardExtraFlags.jar` plugin file
> - **Only keep** `WorldGuardExtraFlagsPlus.jar` on your server
> - Both plugins cannot coexist - they will conflict with each other

- Support for **Folia** ✅
- New Flag **"disable-completely"** | Blocks all usage of specified items (MACE, FIREWORK_ROCKET, WIND_CHARGE, TOTEM_OF_UNDYING, TRIDENT) ✅
  - *Note: `permit-completely` is replaced. Please use `disable-completely` instead.*
- New Flag **"disable-throw"** | Blocks **throwing** egg, snowball, ender pearl, and experience bottle when listed (`EGG`, `SNOWBALL`, `ENDER_PEARL`, `EXPERIENCE_BOTTLE`). Use **`disable-completely`** for tridents, wind charges, and other blocked items ✅
- New Flags **"entry-min-level"** & **"entry-max-level"** | Restrict region entry based on **Player (xp) level** or **PlaceholderAPI** values ✅
- New Flag **"player-count-limit"** | Limit maximum number of players in a region ✅
- New **Configurable Messages** | Customize all plugin messages via `messages-wgefp.yml` in WorldGuard folder (including **`disable-throw-blocked`**, **`disable-completely-blocked`**, etc.) ✅
- New **Message Cooldown System** | Prevents message spam with configurable cooldown (default: 3 seconds) ✅
- **Update Checker** | Automatically checks for updates from Spigot, GitHub, and Modrinth ✅
- **Paper 1.21.x** | `plugin.yml` declares **`api-version: 1.21`** so Paper **1.21.x** servers and forks load the plugin ✅
- New Flag **"villager-trade"** | Control villager trading in regions ✅
- New Flag **"disable-collision"** | Disable player collision in regions ✅
  - *Uses Minecraft's native scoreboard teams to control collision. TAB plugin is supported with API integration. May conflict with other plugins that use teams. See [documentation](public-documents/disable-collision%20flag%20documentation.md) for details.*
- **PlaceholderAPI Chat Integration** | Chat prefix/suffix supports PlaceholderAPI placeholders ✅
- New Flags **"allow-block-place"**, **"deny-block-place"**, **"allow-block-break"**, **"deny-block-break"** | Fine-grained block placement and breaking control ✅
- New Flags **"deny-item-drops"**, **"deny-item-pickup"** | Restrict specific items from being dropped or picked up (works when WorldGuard allows drops/pickups) ✅
- New Flag **"permit-workbenches"** | Block workbench usage (anvil, crafting table, ender chest, etc.) and crafting table crafting in regions ✅
  - *Note: `permit-workbenches CRAFT` now only blocks crafting table (3x3) crafting, not inventory (2x2) crafting. Use `inventory-craft` flag to block inventory crafting.*
- New Flag **"inventory-craft"** | Block inventory crafting (2x2 grid) in regions ✅
- **Godmode & Fly Flag Enhancement** | The `godmode` and `fly` flags now also disable EssentialsX godmode/fly when entering regions with these flags disabled (EssentialsX integration) ✅

## About

WorldGuard allows protecting areas of land by the creation of regions which then can be customized further by applying special flags. 
WorldGuard provides an API that 3th party plugins can use to provide their own flags.

This plugin adds extra flags to allow customizing regions even further.
WorldGuard ExtraFlags Plus is extension to WorldGuard that adds 38+ new flags!

## New updates & features developed by (WorldGuard ExtraFlags Plus)

- tins

## Original author (WorldGuard ExtraFlags)

- isokissa3
- https://joniaromaa.fi

## Support & Community

- 💬 **Discord:** [Join our Discord server](https://discord.gg/TCJAwsdqum)
