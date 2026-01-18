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
		"Disable chambered ender pearl settings",
		"Controls behavior of the disable-chambered-enderpearl flag"
	})
	private DisableChamberedEnderPearlSettings disableChamberedEnderPearl = new DisableChamberedEnderPearlSettings();
	
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
	public static class DisableChamberedEnderPearlSettings {
		@Comment({
			"How long (in seconds) to track ender pearls thrown outside flagged regions",
			"After this time, pearls will no longer be tracked for teleportation prevention",
			"Default: 120 seconds (2 minutes)"
		})
		private int pearlTrackingExpirySeconds = 120;
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
		private boolean disableChamberedEnderpearl = true;
	}
}

