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
		  {required}   - The required level/threshold value
		  {current}    - The player's current level/value
		  {item}       - The item name that was blocked
		  {permission} - The permission that was required/denied
		
		Message Cooldown:
		  Prevents message spam by adding a cooldown between messages sent to the same player.
		  Set to 0 to disable cooldown (messages will always be sent).
		  Default: 3 seconds
		""";
	
	@Comment({
		"Message cooldown in seconds",
		"Prevents message spam by adding a cooldown between messages sent to the same player",
		"Set to 0 to disable cooldown (messages will always be sent)",
		"Default: 2 seconds"
	})
	private int sendMessageCooldown = 2;
	
	@Comment("Entry level flags messages")
	private String entryMinLevelDenied = "&c&lHey! &r&7Sorry, but your level (&8{current}&7/&8{required}&7) is too low to enter this area.";
	
	@Comment("Entry level flags messages")
	private String entryMaxLevelDenied = "&c&lHey! &r&7Sorry, but your level (&8{current}&7/&8{required}&7) is too high to enter this area.";
	
	@Comment("Entry permission flags messages")
	private String entryPermissionDenied = "&c&lHey! &r&7Sorry, but you do not have the required permission (&8{permission}&7) to enter this area.";
	
	@Comment("Entry permission flags messages")
	private String entryDenyPermissionDenied = "&c&lHey! &r&7Sorry, but you can't enter this area with the permission (&8{permission}&7).";

	@Comment("Disable completely flag message")
	private String disableCompletelyBlocked = "&c&lHey! &r&7Sorry, but you can't use &f{item} &7here!";
	
	@Comment("Disable throw flag — egg, snowball, ender pearl, experience bottle (see ThrowableItemFlag)")
	private String disableThrowBlocked = "&c&lHey! &r&7Sorry, but you can't throw &f{item} &7here!";

	@Comment("Permit workbenches flag message")
	private String permitWorkbenchesBlocked = "&c&lHey! &r&7Sorry, but you can't use &f{workbench}&7 here!";
	
	@Comment("Inventory craft flag message")
	private String inventoryCraftBlocked = "&c&lHey! &r&7Sorry, but you can't inventory craft here!";

	@Comment("WorldEdit / FAWE denied by region worldedit flag")
	private String worldeditDenied = "&c&lHey! &r&7Sorry, but you cannot use WorldEdit here.";
	
	@Comment("Godmode disabled message (extra plugins and worldguard)")
	private String godmodeDisabled = "&c&lHey! &r&7Sorry, but god mode is disabled in this region!";

	@Comment({
		"Message shown when player tries to interact with block they're standing on",
		"Helps players understand why bucket interactions fail when standing on water/lava"
	})
	private String standingOnBlockInteraction = "&c&lHey! &r&7Sorry, but you need to move slightly off the block to interact with it!";

	@Comment("Player count limit denied message")
	private String playerCountLimitDenied = "&c&lHey! &r&7Sorry, but this region is full! Maximum players: &f{limit}";
}

