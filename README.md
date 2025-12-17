# WorldGuard ExtraFlags Plus

A Bukkit plugin extension that provides extra flags for [WorldGuard](https://github.com/EngineHub/WorldGuard).

- Support for **Folia** ✅
- New Flag **"disable-completely"** | Blocks all usage of specified items (MACE, FIREWORK_ROCKET, WIND_CHARGE, TOTEM_OF_UNDYING, TRIDENT) ✅
  - *Note: `permit-completely` is replaced. Please use `disable-completely` instead.*
- New Flags **"entry-min-level"** & **"entry-max-level"** | Restrict region entry based on **Player (xp) level** or **PlaceholderAPI** values ✅
- New **Configurable Messages** | Customize all plugin messages via `messages-wgefp.yml` in WorldGuard folder ✅
- New **Message Cooldown System** | Prevents message spam with configurable cooldown (default: 3 seconds) ✅
- **Update Checker** | Automatically checks for updates from Spigot, GitHub, and Modrinth ✅
- New Flag **"villager-trade"** | Control villager trading in regions ✅
- New Flag **"disable-collision"** | Disable player collision in regions ✅
  - *Note: On Folia servers, this flag automatically uses per-player scoreboards (Folia limitation). On Paper/Spigot, it uses the main scoreboard.*
- **PlaceholderAPI Chat Integration** | Chat prefix/suffix supports PlaceholderAPI placeholders ✅
- New Flags **"allow-block-place"**, **"deny-block-place"**, **"allow-block-break"**, **"deny-block-break"** | Fine-grained block placement and breaking control ✅
- New Flags **"deny-item-drops"**, **"deny-item-pickup"** | Restrict specific items from being dropped or picked up (works when WorldGuard allows drops/pickups) ✅
- New Flag **"permit-workbenches"** | Block workbench usage (anvil, crafting table, ender chest, etc.) and crafting table crafting in regions ✅
  - *Note: `permit-workbenches CRAFT` now only blocks crafting table (3x3) crafting, not inventory (2x2) crafting. Use `inventory-craft` flag to block inventory crafting.*
- New Flag **"inventory-craft"** | Block inventory crafting (2x2 grid) in regions ✅
- **Godmode Flag Enhancement** | The `godmode` flag now also disables EssentialsX godmode when entering regions with godmode disabled (EssentialsX integration) ✅

## About

WorldGuard allows protecting areas of land by the creation of regions which then can be customized further by applying special flags. 
WorldGuard provides an API that 3th party plugins can use to provide their own flags.

This plugin adds extra flags to allow customizing regions even further.
WorldGuard ExtraFlags Plus is extension to WorldGuard that adds 37+ new flags!

## New updates & features developed by (WorldGuard ExtraFlags Plus)

- tins

## Original author (WorldGuard ExtraFlags)

- isokissa3
- https://joniaromaa.fi

## Support & Community

- 💬 **Discord:** [Join our Discord server](https://discord.gg/TCJAwsdqum)
