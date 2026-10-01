# WorldGuard ExtraFlags Plus (WGEFP)

WorldGuard ExtraFlags Plus (WGEFP) is a plugin extension for [WorldGuard](https://github.com/EngineHub/WorldGuard) that adds **45+ extra region flags** — item & throwable blocking, entry control, chat formatting, and region command automation.

**Release 4.4.6**

## ⚠️ Warning DO NOT USE BOTH PLUGINS TOGETHER!

> If you're upgrading from `WorldGuardExtraFlags` to `WorldGuardExtraFlagsPlus`, make sure to:
> - **Remove** the old `WorldGuardExtraFlags.jar` plugin file
> - **Only keep** `WorldGuardExtraFlagsPlus.jar` on your server
> - Both plugins cannot coexist - they will conflict with each other

## Feature Highlights

### 🪓 Item & Throwable Control

- **`disable-completely`** — Blocks all usage of listed items: `MACE`, `FIREWORK_ROCKET`, `WIND_CHARGE`, `TOTEM_OF_UNDYING`, `TRIDENT`, `GOAT_HORN`, vanilla spear tiers (1.21.11+), plus `SPEAR` for all spear tiers at once.
  - *Note: `permit-completely` is replaced. Please use `disable-completely` instead.*
  - *Spear **Lunge** (packet `STAB`, including the Lunge enchant) requires **PacketEvents** or **ProtocolLib** on the server for reliable blocking; without either, Lunge may bypass region checks.*
- **`disable-throw`** — Blocks **throwing** `EGG`, `SNOWBALL`, `ENDER_PEARL`, and `EXPERIENCE_BOTTLE`. Use `disable-completely` for tridents, wind charges, and other blocked items.
- **`deny-item-drops`** / **`deny-item-pickup`** — Restrict specific items from being dropped or picked up (works even when WorldGuard allows drops/pickups).

### 🚪 Entry Control

- **`entry-min-level`** / **`entry-max-level`** — Restrict region entry based on player **XP level** or **PlaceholderAPI** values.
- **`entry-permission`** / **`entry-deny-permission`** — Require (or deny) a Bukkit permission node for region entry — e.g. `/rg flag <region> entry-permission myPlugin.myPermission.1`.
- **`player-count-limit`** — Limit the maximum number of players inside a region.

### ⚙️ Region Automation

- **`command-on-entry`** / **`command-on-exit`** — Run player commands when entering or leaving a region.
- **`console-command-on-entry`** / **`console-command-on-exit`** — Run console commands on region entry/exit.
- **`console-command-repeat`** — Repeats a console command at a fixed interval (1–60 seconds) while a player stays in the region. Disabled by default — enable via `all-flags-control.console-command-repeat: true`. Format: `/rg flag <region> console-command-repeat "20 give %player% diamond 1"`.
- **`console-command-timer`** — Repeats a console command at a fixed interval (1–300 seconds) while a player stays in the region. Timer resets on exit (hold-to-earn / KOTH semantics). Disabled by default — enable via `all-flags-control.console-command-timer: true`. Format: `/rg flag <region> console-command-timer "60 give %player% diamond 1"`.
- Command flags (`command-on-entry/exit`, `console-command-on-entry/exit`, `console-command-repeat`, `console-command-timer`) support `%player%`, `%username%`, `%uuid%`, and `{uuid}` placeholders.
- **`chat-prefix`** / **`chat-suffix`** — Per-region chat formatting with full **PlaceholderAPI** placeholder support.

### 🧱 Block & World Interaction

- **`allow-block-place`** / **`deny-block-place`** / **`allow-block-break`** / **`deny-block-break`** — Fine-grained block placement and breaking control, including bucket fill/empty and waterlogged blocks. Optional `require-membership` restricts these to region members. `all-flags-control.check-order` in `config-wgefp.yml` (default `allow-first`, or `deny-first`) controls whether the allow or deny flags are evaluated first when both are set. Waterlogged checks: `allow-block-break-settings.waterlogged-material-check` (`both` | `host` | `liquid`, default `both`).
- **`enderchest-access`** — Control ender chest access per region (like WorldGuard `chest-access`, ender chests only). Enabled by default (`all-flags-control.enderchest-access: true`); set `deny` to block.
- **`permit-workbenches`** — Block workbench usage (anvil, crafting table, ender chest, etc.) and crafting table crafting in regions.
  - *Note: `permit-workbenches CRAFT` now only blocks crafting table (3x3) crafting, not inventory (2x2) crafting. Use the `inventory-craft` flag to block inventory crafting.*
- **`inventory-craft`** — Block inventory crafting (2x2 grid) in regions.
- **`villager-trade`** — Control villager trading in regions.
- **`disable-collision`** — Disable player collision in regions.
  - *Uses Minecraft's native scoreboard teams to control collision. TAB plugin is supported with API integration. May conflict with other plugins that use teams. See [disable-collision documentation](https://tinsware.github.io/wiki/docs/games/minecraft/plugins/worldguard-extraflags-plus/flags-reference#disable-collision) for details.*
- **`chunk-unload`**, **`nether-portals`**, **`worldedit`**, **`lightning-damage`**, **`frostwalker`**, **`play-sounds`** — World interaction flags: chunk unload control, portal use, WorldEdit/FAWE gating, visual-only lightning (ideal for PvP arenas), frostwalker control, and per-region sound effects.

### 🧟 Mob & Spawn Control

- **`deny-mobspawn`** — Blocks spawning of mobs in the listed categories inside a region. Categories: `ALL_HOSTILE` (every hostile `Enemy` mob, bosses like `WITHER` and `ENDER_DRAGON` included), `ALL_NEUTRAL` (mobs that only attack when provoked), `ALL_PEACEFUL` (every other living mob: villagers, animals, fish, bats, allays, armor stands, ...), `ALL` (all three), or raw `EntityType` names (`ZOMBIE`, `CREEPER`) for per-mob control. Use `clear` to reset. See [deny-mobspawn categories](https://tinsware.github.io/wiki/docs/games/minecraft/plugins/worldguard-extraflags-plus/deny-mobspawn) for exactly which mobs each category includes.

### 🛡️ Player Protection & Effects

- **`godmode`** — Immortality in regions. Also disables EssentialsX godmode/fly when entering regions where these flags are disabled (EssentialsX integration).
- **`keep-inventory`** / **`keep-exp`** — Keep items or XP on death. DeluxeCombat restores inventory when combat-logging in `keep-inventory` regions.
- **`item-durability`** — Control item durability loss.
- **`fly`**, **`glide`**, **`walk-speed`**, **`fly-speed`** — Movement and speed control.
- **`blocked-effects`** / **`give-effects`** — Block or grant potion effects per region. `give-effects` accepts `night_vision` / `minecraft:night_vision` ([#15](https://github.com/tinsware/WorldGuardExtraFlagsPlus/issues/15)).
- **Session tick performance** — `blocked-effects`, `give-effects`, and `play-sounds` handlers skip work when the flags are inactive.

### ✨ Extras

- **Folia support** — fully compatible with async region handling (FoliaLib shaded in).
- **Paper 1.21 – 26.2+** — `plugin.yml` declares `api-version: 1.21`; Java 21 bytecode.
- **Configurable messages + cooldown** — customize every plugin message via `messages-wgefp.yml`, with a message cooldown (default: 2 seconds) to prevent spam.
- **Update checker** — automatically checks for updates from Spigot, GitHub, and Modrinth.
- **Quiet startup** — `verbose-startup-logs: false` by default ([#14](https://github.com/tinsware/WorldGuardExtraFlagsPlus/issues/14)).
- **`hide-players`** *(experimental)* — hub/lobby visibility optimization, opt-in in config.
- **`chambered-enderpearl`** *(experimental)* — mitigates chambered ender pearl bypasses (pearls thrown outside denied regions are removed when the shooter enters a region where the flag denies).
- **`teleport-on-entry`** / **`teleport-on-exit`**, **`join-location`** *(not on Folia)*, **`respawn-location`** — location-based flags.

## Quick Start

1. Drop `WorldGuardExtraFlagsPlus.jar` into your server's `plugins/` folder.
2. Restart the server or run `/wg reload`.
3. Apply flags with the standard WorldGuard syntax: `/rg flag <region> <flag> <value>`.

📖 **Full documentation:** [tinsware Wiki — WorldGuard ExtraFlags Plus](https://tinsware.github.io/wiki/docs/games/minecraft/plugins/worldguard-extraflags-plus/) — flag usage examples, reference, configuration, and deny-mobspawn categories.

## Configuration

- **`plugins/WorldGuard/config-wgefp.yml`** — plugin toggles (e.g. `all-flags-control.console-command-repeat`, `all-flags-control.console-command-timer`, `all-flags-control.check-order`, `require-membership`, `hide-players`, `verbose-startup-logs`).
- **`plugins/WorldGuard/messages-wgefp.yml`** — all plugin messages. Placeholders: `{permission}`, `{required}`, `{current}`, `{item}`, `{workbench}`; message cooldown (default: 2 seconds) prevents spam.
- Reload instantly with `/wgefp reload` or `/wg reload`.

## PlaceholderAPI

Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to use the plugin-provided placeholder:

- **`%wgefp_players_in_region_<regionId>%`** — number of online players currently inside the region with the given id. Region ids are case-insensitive, and players are summed across **all worlds** that have a region with that id. Example: `%wgefp_players_in_region_spawn%` → `3`. Returns `0` when no such region exists or nobody is inside.
- Counts are cached and refreshed once per second (Folia-safe — each player is counted on their own region thread).
- Toggle with `all-flags-control.papi-placeholders` in `plugins/WorldGuard/config-wgefp.yml` (default: `false` — opt-in, enable to register the placeholder).

## Compatibility

- **Minecraft:** 1.21 – 26.2+
- **WorldGuard:** 7.0.15+
- **`plugin.yml`:** `api-version: 1.21`, Java 21 bytecode, **Folia supported**
- **Soft dependencies:** ProtocolLib, PacketEvents, PlaceholderAPI, Essentials/EssentialsX, TAB, DeluxeCombat

## Authors

- **Active Author & Developer of WGEFP:** [tinsware](https://tinsware.github.io/)
- **Original author:** isokissa3 (https://joniaromaa.fi)

## Support & Community

- 📜 **Release notes:** [GitHub Releases](https://github.com/tinsware/WorldGuardExtraFlagsPlus/releases) (latest: **4.4.6**)
- 💬 **Discord:** [Join our Discord server](https://tinsware.github.io/discord)
