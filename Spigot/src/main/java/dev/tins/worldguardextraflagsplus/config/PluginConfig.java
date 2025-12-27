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
		"Join location settings",
		"Controls behavior of the join-location flag"
	})
	private JoinLocationSettings joinLocation = new JoinLocationSettings();
	
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
	public static class JoinLocationSettings {
		@Comment({
			"If true, enables the join-location flag functionality",
			"If false, disables the join-location flag to avoid deprecation warnings on Paper servers",
			"Warning: On Paper servers, enabling this will show a deprecation warning but functionality works correctly"
		})
		private boolean enabled = true;
	}
}

