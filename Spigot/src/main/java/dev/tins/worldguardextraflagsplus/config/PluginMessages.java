package dev.tins.worldguardextraflagsplus.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Plugin messages configuration class.
 * Uses ConfigLib for automatic YAML handling.
 * 
 * Note: Empty strings are treated as disabled messages.
 * Placeholders: {required}, {current}, {item}, {workbench}
 */
@Getter
@NoArgsConstructor
@Configuration
public final class PluginMessages {
	
	public static final String MESSAGES_HEADER = """
		WorldGuardExtraFlagsPlus Messages Configuration
		This file is located in the WorldGuard plugin folder
		You can customize all messages shown to players here
		
		IMPORTANT: Empty String = Disabled Message
		If you set a message to "" (empty string), that message will be disabled
		and no message will be sent to the player. This is useful if you want to
		silence certain messages while keeping others active.
		
		Example:
		  disable-completely-blocked: ""  # This will disable the blocked item message
		
		Messages are cached on plugin load for better performance.
		Use /wg reload or /wgefp reload (or /wgefplus reload or /worldguardextraflagsplus reload) to reload messages without restarting the server.
		
		Placeholders:
		  {required} - The required level/threshold value
		  {current}  - The player's current level/value
		  {item}     - The item name that was blocked
		
		Message Cooldown:
		  Prevents message spam by adding a cooldown between messages sent to the same player.
		  Set to 0 to disable cooldown (messages will always be sent).
		  Default: 3 seconds
		""";
	
	@Comment({
		"Message cooldown in seconds",
		"Prevents message spam by adding a cooldown between messages sent to the same player",
		"Set to 0 to disable cooldown (messages will always be sent)",
		"Default: 3 seconds"
	})
	private int sendMessageCooldown = 3;
	
	@Comment("Entry level flags messages")
	private String entryMinLevelDenied = "&cYour level (&7{current}&c) is low to enter this area. &7Min: &8{required}";
	
	@Comment("Entry level flags messages")
	private String entryMaxLevelDenied = "&cYour level (&7{current}&c) is so high to enter this area. &7Max: &8{required}";
	
	@Comment("Disable completely flag message")
	private String disableCompletelyBlocked = "&cHey! &7You can not use {item} in here!";
	
	@Comment("Permit workbenches flag message")
	private String permitWorkbenchesBlocked = "&cHey! &7You can not use {workbench} in here!";
	
	@Comment("Inventory craft flag message")
	private String inventoryCraftBlocked = "&cHey! &7You can not craft items in your inventory here!";
	
	@Comment("Godmode disabled message (extra plugins and worldguard)")
	private String godmodeDisabled = "&cHey! &7Godmode disabled in this region!";
}

