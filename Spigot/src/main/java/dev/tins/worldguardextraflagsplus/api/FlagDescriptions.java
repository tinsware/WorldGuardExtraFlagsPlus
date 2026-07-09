package dev.tins.worldguardextraflagsplus.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry of human-readable metadata for every flag in WorldGuardExtraFlagsPlus.
 *
 * <p>External plugins — such as WG-GUI — can call {@link #getAll()} (via a direct dependency or
 * reflection) to display flag descriptions, accepted values, and usage examples inside their own
 * UIs without having to hardcode any flag knowledge.
 *
 * <p>This class is also used internally by the plugin (e.g. the {@code /wgefp flags} command).
 *
 * <p><b>Maintainers:</b> when adding a new flag to {@code Flags.java}, add a corresponding entry
 * here so the metadata stays complete.
 */
public final class FlagDescriptions
{
	private static final Map<String, FlagMeta> ALL = new LinkedHashMap<>();

	static
	{
		// ── LOCATION & TELEPORTATION ────────────────────────────────────
		ALL.put("teleport-on-entry", new FlagMeta(
				"Teleports a player to the specified location when they enter the region.",
				"world,x,y,z,yaw,pitch",
				"/rg flag <region> teleport-on-entry world,0,64,0,0,0"));

		ALL.put("teleport-on-exit", new FlagMeta(
				"Teleports a player to the specified location when they leave the region.",
				"world,x,y,z,yaw,pitch",
				"/rg flag <region> teleport-on-exit world,0,64,0,0,0"));

		ALL.put("join-location", new FlagMeta(
				"Sets the respawn/join location for players who log in inside this region.",
				"world,x,y,z,yaw,pitch",
				"/rg flag <region> join-location world,0,64,0,0,0"));

		ALL.put("respawn-location", new FlagMeta(
				"Overrides the player's spawn point when they die inside the region.",
				"world,x,y,z,yaw,pitch",
				"/rg flag <region> respawn-location world,0,64,0,0,0"));

		// ── COMMAND EXECUTION ───────────────────────────────────────────
		ALL.put("command-on-entry", new FlagMeta(
				"Executes commands as the player when they enter the region. Supports %player% and {player} placeholders.",
				"command string",
				"/rg flag <region> command-on-entry \"say Welcome, %player%!\""));

		ALL.put("command-on-exit", new FlagMeta(
				"Executes commands as the player when they leave the region. Supports %player% and {player} placeholders.",
				"command string",
				"/rg flag <region> command-on-exit \"say Goodbye, %player%!\""));

		ALL.put("console-command-on-entry", new FlagMeta(
				"Executes commands from the console when a player enters the region. Supports %player% and {player} placeholders.",
				"command string",
				"/rg flag <region> console-command-on-entry \"eco give %player% 100\""));

		ALL.put("console-command-on-exit", new FlagMeta(
				"Executes commands from the console when a player leaves the region. Supports %player% and {player} placeholders.",
				"command string",
				"/rg flag <region> console-command-on-exit \"eco take %player% 100\""));

		// ── MOVEMENT & SPEED CONTROL ────────────────────────────────────
		ALL.put("walk-speed", new FlagMeta(
				"Sets the player's walk speed while inside the region. Values range from -1.0 to 1.0 (default is 0.2).",
				"decimal (-1.0 to 1.0)",
				"/rg flag <region> walk-speed 0.5"));

		ALL.put("fly-speed", new FlagMeta(
				"Sets the player's fly speed while inside the region. Values range from -1.0 to 1.0 (default is 0.1).",
				"decimal (-1.0 to 1.0)",
				"/rg flag <region> fly-speed 0.3"));

		ALL.put("fly", new FlagMeta(
				"Controls whether players can fly in the region. When denied, EssentialsX flight is also disabled.",
				"allow / deny",
				"/rg flag <region> fly deny"));

		ALL.put("glide", new FlagMeta(
				"Controls elytra gliding in the region. 'force' disables elytra entirely, 'deny' prevents deploying, 'allow' permits it.",
				"allow / deny / force",
				"/rg flag <region> glide force"));

		ALL.put("frostwalker", new FlagMeta(
				"Controls whether the Frost Walker enchantment can create frosted ice in the region.",
				"allow / deny",
				"/rg flag <region> frostwalker deny"));

		// ── PROTECTION & SURVIVAL ───────────────────────────────────────
		ALL.put("godmode", new FlagMeta(
				"Grants invulnerability to players inside the region (all damage cancelled). EssentialsX godmode is also toggled.",
				"allow / deny",
				"/rg flag <region> godmode allow"));

		ALL.put("keep-inventory", new FlagMeta(
				"Preserves the player's inventory on death while inside the region. Optionally integrates with DeluxeCombat for combat-log restore.",
				"true / false",
				"/rg flag <region> keep-inventory true"));

		ALL.put("keep-exp", new FlagMeta(
				"Preserves the player's experience levels on death while inside the region.",
				"true / false",
				"/rg flag <region> keep-exp true"));

		ALL.put("item-durability", new FlagMeta(
				"Controls whether item durability decreases when used inside the region.",
				"allow / deny",
				"/rg flag <region> item-durability deny"));

		// ── CHAT MODIFICATION ───────────────────────────────────────────
		ALL.put("chat-prefix", new FlagMeta(
				"Sets a chat prefix that is prepended to messages sent by players in the region. Supports PlaceholderAPI placeholders.",
				"text",
				"/rg flag <region> chat-prefix \"&7[Region]&\""));

		ALL.put("chat-suffix", new FlagMeta(
				"Sets a chat suffix that is appended to messages sent by players in the region. Supports PlaceholderAPI placeholders.",
				"text",
				"/rg flag <region> chat-suffix \" &7[Region]\""));

		// ── EFFECT CONTROL ──────────────────────────────────────────────
		ALL.put("blocked-effects", new FlagMeta(
				"Blocks specified potion effects from applying to players in the region. Use potion effect names like BLINDNESS, POISON.",
				"potion effect names (comma-separated)",
				"/rg flag <region> blocked-effects BLINDNESS,SLOWNESS"));

		ALL.put("give-effects", new FlagMeta(
				"Applies specified potion effects to players while they remain in the region. Accepts effect:amplifier:particles format.",
				"potion effect names (comma-separated)",
				"/rg flag <region> give-effects night_vision,strength:1:false"));

		ALL.put("play-sounds", new FlagMeta(
				"Plays sounds to players while they are inside the region. Useful for ambient or warning sounds.",
				"sound data",
				"/rg flag <region> play-sounds minecraft:block.note_block.pling"));

		// ── WORLD INTERACTION ───────────────────────────────────────────
		ALL.put("worldedit", new FlagMeta(
				"Controls whether WorldEdit operations are allowed in the region. Bypass permission: worldguardextraflagsplus.worldedit.bypass.",
				"allow / deny",
				"/rg flag <region> worldedit deny"));

		ALL.put("nether-portals", new FlagMeta(
				"Controls whether nether portals can be created or used in the region.",
				"allow / deny",
				"/rg flag <region> nether-portals deny"));

		ALL.put("chunk-unload", new FlagMeta(
				"When denied, prevents chunks in the region from unloading — useful for keeping redstone and farms active.",
				"allow / deny",
				"/rg flag <region> chunk-unload deny"));

		ALL.put("villager-trade", new FlagMeta(
				"Controls whether players can trade with villagers inside the region.",
				"allow / deny",
				"/rg flag <region> villager-trade deny"));

		ALL.put("inventory-craft", new FlagMeta(
				"Blocks inventory (2x2 grid) crafting in the region. Use permit-workbenches CRAFT to block crafting table (3x3) crafting.",
				"allow / deny",
				"/rg flag <region> inventory-craft deny"));

		// ── BLOCK & ITEM CONTROL ────────────────────────────────────────
		ALL.put("allow-block-place", new FlagMeta(
				"Only the specified materials can be placed in the region. All other block placement is denied. Optionally member-only.",
				"material names (comma-separated)",
				"/rg flag <region> allow-block-place OAK_PLANKS,STONE"));

		ALL.put("deny-block-place", new FlagMeta(
				"Prevents placing the specified materials in the region. All other block placement is allowed.",
				"material names (comma-separated)",
				"/rg flag <region> deny-block-place TNT,LAVA"));

		ALL.put("allow-block-break", new FlagMeta(
				"Only the specified materials can be broken in the region. All other block breaking is denied. Optionally member-only.",
				"material names (comma-separated)",
				"/rg flag <region> allow-block-break GLASS,GLASS_PANE"));

		ALL.put("deny-block-break", new FlagMeta(
				"Prevents breaking the specified materials in the region. All other block breaking is allowed.",
				"material names (comma-separated)",
				"/rg flag <region> deny-block-break DIAMOND_BLOCK,EMERALD_BLOCK"));

		ALL.put("deny-item-drops", new FlagMeta(
				"Prevents players from dropping the specified items while in the region.",
				"material names (comma-separated)",
				"/rg flag <region> deny-item-drops DIAMOND,NETHERITE_INGOT"));

		ALL.put("deny-item-pickup", new FlagMeta(
				"Prevents players from picking up the specified items while in the region.",
				"material names (comma-separated)",
				"/rg flag <region> deny-item-pickup DIAMOND,NETHERITE_INGOT"));

		ALL.put("disable-completely", new FlagMeta(
				"Blocks all usage of specified items: MACE, FIREWORK_ROCKET, WIND_CHARGE, TOTEM_OF_UNDYING, TRIDENT, SPEAR (all vanilla tiers) and individual spear types. Requires PacketEvents/ProtocolLib for full STAB/Lunge protection.",
				"MACE, FIREWORK_ROCKET, WIND_CHARGE, TOTEM_OF_UNDYING, TRIDENT, SPEAR, (spear tiers)",
				"/rg flag <region> disable-completely MACE,SPEAR,TRIDENT"));

		ALL.put("disable-throw", new FlagMeta(
				"Blocks throwing (launching) of specified items: EGG, SNOWBALL, ENDER_PEARL, EXPERIENCE_BOTTLE. Use disable-completely for tridents and wind charges.",
				"EGG, SNOWBALL, ENDER_PEARL, EXPERIENCE_BOTTLE",
				"/rg flag <region> disable-throw ENDER_PEARL,EGG"));

		ALL.put("permit-workbenches", new FlagMeta(
				"Controls which workbench types (anvil, crafting table, ender chest, etc.) are blocked in the region. Use ALL to block everything, CLEAR to reset.",
				"ALL, CLEAR, ANVIL, CARTOGRAPHY, CRAFT, ENDER, GRINDSTONE, LOOM, SMITHING, STONECUTTER",
				"/rg flag <region> permit-workbenches ALL"));

		// ── ENTRY CONTROL ───────────────────────────────────────────────
		ALL.put("entry-min-level", new FlagMeta(
				"Sets the minimum player XP level (or PlaceholderAPI integer value) required to enter the region.",
				"number or PlaceholderAPI placeholder",
				"/rg flag <region> entry-min-level 10"));

		ALL.put("entry-max-level", new FlagMeta(
				"Sets the maximum player XP level (or PlaceholderAPI integer value) allowed to enter the region.",
				"number or PlaceholderAPI placeholder",
				"/rg flag <region> entry-max-level 50"));

		ALL.put("player-count-limit", new FlagMeta(
				"Limits the maximum number of players that can be inside the region at the same time.",
				"integer",
				"/rg flag <region> player-count-limit 10"));

		// ── SPECIAL FEATURES ────────────────────────────────────────────
		ALL.put("disable-collision", new FlagMeta(
				"When set to true, disables player collision in the region. Uses Minecraft scoreboard teams. Supports TAB plugin integration.",
				"true / false",
				"/rg flag <region> disable-collision true"));

		ALL.put("hide-players", new FlagMeta(
				"When set to true, hides players from each other in the region. Useful for hub/lobby areas. Experimental — opt-in via config.",
				"true / false",
				"/rg flag <region> hide-players true"));

		ALL.put("chambered-enderpearl", new FlagMeta(
				"Mitigates chambered ender pearl bypasses. Pearls thrown outside denied regions are removed when the shooter enters a region where the flag denies. Experimental.",
				"allow / deny",
				"/rg flag <region> chambered-enderpearl deny"));

		ALL.put("lightning-damage", new FlagMeta(
				"Controls whether lightning strikes deal damage to players. When denied, lightning appears visually but causes no damage. Ideal for PvP arenas.",
				"allow / deny",
				"/rg flag <region> lightning-damage deny"));
	}

	private FlagDescriptions()
	{
	}

	/**
	 * Returns an unmodifiable map of all flag metadata, keyed by flag name (kebab-case).
	 * The map preserves insertion order (same order as {@code Flags.java}).
	 */
	public static Map<String, FlagMeta> getAll()
	{
		return Collections.unmodifiableMap(ALL);
	}

	/**
	 * Returns metadata for a single flag, or {@code null} if the flag is unknown.
	 *
	 * @param flagName the flag name in kebab-case (e.g. "godmode", "allow-block-break")
	 */
	public static FlagMeta get(String flagName)
	{
		return ALL.get(flagName);
	}

	/**
	 * Metadata for a single region flag.
	 *
	 * @param description   what the flag does (short, user-facing explanation)
	 * @param acceptedValues what values the flag accepts (type + range or list of valid values)
	 * @param example       an example {@code /rg flag} command showing typical usage
	 */
	public record FlagMeta(
			String description,
			String acceptedValues,
			String example)
	{
	}
}
