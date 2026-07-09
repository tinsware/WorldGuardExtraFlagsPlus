package dev.tins.worldguardextraflagsplus.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Main plugin configuration class.
 * Uses ConfigLib for automatic YAML handling.
 */
@Getter
@NoArgsConstructor
@Configuration
public final class PluginConfig {
	
	public static final String CONFIG_HEADER = """
		WorldGuardExtraFlagsPlus Configuration
		This file is located in the WorldGuard plugin folder
		You can customize plugin behavior here
		
		IMPORTANT: This file is reloaded when you use /wg reload or /wgefp reload
		
		=============================================================================
		INDIVIDUAL FLAG SETTINGS
		=============================================================================
		Additional configuration for flags that need extra settings
		""";
	
	@Comment({
		"Permit workbenches settings",
		"Controls behavior of the permit-workbenches flag"
	})
	private PermitWorkbenchesSettings permitWorkbenches = new PermitWorkbenchesSettings();
	
	@Comment({
		"Godmode settings",
		"Controls behavior of the godmode flag"
	})
	private GodmodeSettings godmode = new GodmodeSettings();

	@Comment({
		"Keep-inventory settings",
		"Controls behavior of the keep-inventory flag"
	})
	private KeepInventorySettings keepInventorySettings = new KeepInventorySettings();

	@Comment({
		"Allow-block-place settings",
		"Controls behavior of the allow-block-place flag"
	})
	private AllowBlockPlaceSettings allowBlockPlaceSettings = new AllowBlockPlaceSettings();

	@Comment({
		"Allow-block-break settings",
		"Controls behavior of the allow-block-break flag"
	})
	private AllowBlockBreakSettings allowBlockBreakSettings = new AllowBlockBreakSettings();

	@Comment({
		"WorldEdit flag settings",
		"Usage notes for the worldedit region flag"
	})
	private WorldeditSettings worldeditSettings = new WorldeditSettings();

	@Comment({
		"Logging",
		"-------"
	})
	private LoggingSettings logging = new LoggingSettings();

	@Getter
	@NoArgsConstructor
	@Configuration
	public static class LoggingSettings
	{
		@Comment({
			"Print detailed startup lines (config paths, per-region chunk-unload tickets, collision banner).",
			"Default: false — keeps console quiet on large servers (see GitHub issue #14)."
		})
		private boolean verboseStartupLogs = false;
	}

	
	@Getter
	@NoArgsConstructor
	@Configuration
	public static class PermitWorkbenchesSettings {
		@Comment({
			"If true, blocks placement of workbench blocks in regions with permit-workbenches flag",
			"Default: false"
		})
		private boolean permitWorkbenchBlockPlacementToo = false;
		
		@Comment({
			"If true, \"ALL\" keyword in permit-workbenches flag will also include ender chests",
			"Default: false (ALL does not include ender chests by default)"
		})
		private boolean permitAllIncludesEnderchest = false;
	}
	
	@Getter
	@NoArgsConstructor
	@Configuration
	public static class GodmodeSettings {
		@Comment({
			"If true, automatically restore godmode when player leaves a region",
			"Default: false"
		})
		private boolean autoGiveGodmodeRegionLeft = false;
	}

	@Getter
	@NoArgsConstructor
	@Configuration
	public static class KeepInventorySettings {
		@Comment({
			"When true and DeluxeCombat is installed, combat-logging inside a keep-inventory region",
			"saves the player's inventory and restores it on next login.",
			"Default: true"
		})
		private boolean combatLogRestore = true;
	}

	@Getter
	@NoArgsConstructor
	@Configuration
	public static class AllowBlockPlaceSettings {
		@Comment({
			"When true, allow-block-place only applies to region members and owners.",
			"Use with a material whitelist (e.g. OAK_WALL_SIGN) so only members can place those blocks.",
			"Default: false"
		})
		private boolean requireMembership = false;
	}

	@Getter
	@NoArgsConstructor
	@Configuration
	public static class AllowBlockBreakSettings {
		@Comment({
			"When true, allow-block-break only applies to region members and owners.",
			"Default: false"
		})
		private boolean requireMembership = false;
	}

	@Getter
	@NoArgsConstructor
	@Configuration
	public static class WorldeditSettings {
		@Comment({
			"Allow WorldEdit in __global__: /rg flag __global__ worldedit allow",
			"Child regions with worldedit deny still block edits (WorldGuard priority applies).",
			"Bypass permission: worldguardextraflagsplus.worldedit.bypass"
		})
		private boolean documentationOnly = true;
	}


	@Comment({
    " ",
		"=============================================================================",
		"MASTER FLAG CONTROL",
    "=============================================================================",
    "Enable/disable all available flags here",
    "Set to true to enable, false to disable",
    "Disabled flags won't be registered (no deprecation warnings on Paper servers)"
	})
	private AllFlagsControl allFlagsControl = new AllFlagsControl();

	@Getter
	@NoArgsConstructor
	@Configuration
	public static class AllFlagsControl {
		@Comment({
			"LOCATION & TELEPORTATION",
			"------------------------"
		})
		private boolean teleportOnEntry = true;
		private boolean teleportOnExit = true;
		@Comment("By default its true, if you are not using and want to turn off warns set it to false")
		private boolean joinLocation = true;
		private boolean respawnLocation = true;

		@Comment({
			"COMMAND EXECUTION",
			"-----------------"
		})
		private boolean commandOnEntry = true;
		private boolean commandOnExit = true;
		private boolean consoleCommandOnEntry = true;
		private boolean consoleCommandOnExit = true;

		@Comment({
			"MOVEMENT & SPEED CONTROL",
			"------------------------"
		})
		private boolean walkSpeed = true;
		private boolean flySpeed = true;
		private boolean fly = true;
		@Comment({
			"When false, glide is not registered — remove glide from existing regions or WorldGuard will warn (issue #13)."
		})
		private boolean glide = true;
		private boolean frostwalker = true;

		@Comment({
			"PROTECTION & SURVIVAL",
			"---------------------"
		})
		private boolean godmode = true;
		private boolean keepInventory = true;
		private boolean keepExp = true;
		private boolean itemDurability = true;

		@Comment({
			"CHAT MODIFICATION",
			"-----------------"
		})
		private boolean chatPrefix = true;
		private boolean chatSuffix = true;

		@Comment({
			"EFFECT CONTROL",
			"--------------"
		})
		private boolean blockedEffects = true;
		@Comment({
			"Effect syntax: /rg flag <region> give-effects night_vision",
			"Also accepts minecraft:night_vision and optional amplifier/particles."
		})
		private boolean giveEffects = true;

		@Comment({
			"WORLD INTERACTION",
			"-----------------"
		})
		private boolean worldedit = true;
		private boolean playSounds = true;
		private boolean netherPortals = true;
		private boolean chunkUnload = true;
		private boolean villagerTrade = true;
		private boolean inventoryCraft = true;

		@Comment({
			"BLOCK & ITEM CONTROL",
			"-------------------"
		})
		private boolean allowBlockPlace = true;
		private boolean denyBlockPlace = true;
		private boolean allowBlockBreak = true;
		private boolean denyBlockBreak = true;
		private boolean denyItemDrops = true;
		private boolean denyItemPickup = true;
		private boolean disableCompletely = true;
		private boolean disableThrow = true;
		private boolean permitWorkbenches = true;

		@Comment({
			"ENTRY CONTROL",
			"-------------"
		})
		private boolean entryMinLevel = true;
		private boolean entryMaxLevel = true;
		private boolean playerCountLimit = true;

		@Comment({
			"SPECIAL FEATURES",
			"----------------"
		})
		private boolean disableCollision = true;
		@Comment({
			"EXPERIMENTAL — chambered-enderpearl flag",
			"Mitigation for chambered ender pearl bypasses; behavior may change in future releases.",
			"Default: true"
		})
		private boolean chamberedEnderPearl = true;
		@Comment({
			"EXPERIMENTAL — hide-players flag",
			"Hides players from others while inside a hide-players region (hub/lobby optimization).",
			"Default: false — enable here and set hide-players: true on regions to use."
		})
		private boolean hidePlayers = false;

		@Comment({
			"WEATHER & ENVIRONMENTAL DAMAGE",
			"------------------------------",
			"lightning-damage flag",
			"When set to deny on a region, players won't take damage from lightning strikes",
			"but the visual effect is still shown. Useful for PvP arenas.",
			"Usage: /rg flag <region> lightning-damage deny",
			"Default: true"
		})
		private boolean lightningDamage = true;
	}
}

