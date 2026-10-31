package dev.tins.worldguardextraflagsplus.flags;

import dev.tins.worldguardextraflagsplus.flags.helpers.*;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.CommandStringFlag;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.LocationFlag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.Flag;

import dev.tins.worldguardextraflagsplus.flags.data.SoundData;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

public final class Flags
{
	public final static LocationFlag TELEPORT_ON_ENTRY = new LocationFlag("teleport-on-entry");
	public final static LocationFlag TELEPORT_ON_EXIT = new LocationFlag("teleport-on-exit");
	
	public final static SetFlag<String> COMMAND_ON_ENTRY = new CustomSetFlag("command-on-entry", new CommandStringCaseSensitiveFlag(null));
	public final static SetFlag<String> COMMAND_ON_EXIT = new CustomSetFlag("command-on-exit", new CommandStringCaseSensitiveFlag(null));
	
	public final static SetFlag<String> CONSOLE_COMMAND_ON_ENTRY = new CustomSetFlag("console-command-on-entry", new CommandStringCaseSensitiveFlag(null));
	public final static SetFlag<String> CONSOLE_COMMAND_ON_EXIT = new CustomSetFlag("console-command-on-exit", new CommandStringCaseSensitiveFlag(null));
	
	public final static DoubleFlag WALK_SPEED = new DoubleFlag("walk-speed");
	public final static DoubleFlag FLY_SPEED = new DoubleFlag("fly-speed");
	
	public final static BooleanFlag KEEP_INVENTORY = new BooleanFlag("keep-inventory");
	public final static BooleanFlag KEEP_EXP = new BooleanFlag("keep-exp");
	
	public final static StringFlag CHAT_PREFIX = new StringFlag("chat-prefix");
	public final static StringFlag CHAT_SUFFIX = new StringFlag("chat-suffix");
	
	public final static SetFlag<PotionEffectType> BLOCKED_EFFECTS = new SetFlag("blocked-effects", new PotionEffectTypeFlag(null));
	
	public final static StateFlag GODMODE = new StateFlag("godmode", false);
	
	public final static LocationFlag RESPAWN_LOCATION = new LocationFlag("respawn-location");
	
	public final static StateFlag WORLDEDIT = new StateFlag("worldedit", true);
	
	public final static SetFlag<PotionEffect> GIVE_EFFECTS = new SetFlag("give-effects", new PotionEffectFlag(null));
	
	public final static StateFlag FLY = new StateFlag("fly", false);
	
	public final static SetFlag<SoundData> PLAY_SOUNDS = new SetFlag("play-sounds", new SoundDataFlag(null));
	
	public final static StateFlag FROSTWALKER = new StateFlag("frostwalker", true);
	
	public final static StateFlag NETHER_PORTALS = new StateFlag("nether-portals", true);

	public final static SetFlag<Material> ALLOW_BLOCK_PLACE = new SetFlag<Material>("allow-block-place", new BlockMaterialFlag(null));
	public final static SetFlag<Material> DENY_BLOCK_PLACE = new SetFlag<Material>("deny-block-place", new BlockMaterialFlag(null));
	public final static SetFlag<Material> ALLOW_BLOCK_BREAK = new SetFlag<Material>("allow-block-break", new BlockMaterialFlag(null));
	public final static SetFlag<Material> DENY_BLOCK_BREAK = new SetFlag<Material>("deny-block-break", new BlockMaterialFlag(null));

	public final static SetFlag<Material> DENY_ITEM_DROPS = new SetFlag<Material>("deny-item-drops", new MaterialFlag(null));
	public final static SetFlag<Material> DENY_ITEM_PICKUP = new SetFlag<Material>("deny-item-pickup", new MaterialFlag(null));

	public final static ForcedStateFlag GLIDE = new ForcedStateFlag("glide");
	
	public final static StateFlag CHUNK_UNLOAD = new StateFlag("chunk-unload", true);
	
	public final static StateFlag ITEM_DURABILITY = new StateFlag("item-durability", true);
	
	public final static LocationFlag JOIN_LOCATION = new LocationFlag("join-location");
	
	public final static StateFlag VILLAGER_TRADE = new StateFlag("villager-trade", true);

	/**
	 * Controls whether players can open ender chests in this region.
	 * Default is allow — set to deny to block ender chest access (like WorldGuard chest-access, but ender chests only).
	 * Usage: /rg flag &lt;region&gt; enderchest-access deny
	 */
	public final static StateFlag ENDERCHEST_ACCESS = new StateFlag("enderchest-access", true);
	
	public final static StateFlag INVENTORY_CRAFT = new StateFlag("inventory-craft", false);
	
	public final static BooleanFlag DISABLE_COLLISION = new BooleanFlag("disable-collision");

	public final static BooleanFlag HIDE_PLAYERS = new BooleanFlag("hide-players");

	public final static SetFlag<String> DISABLE_COMPLETELY = new CustomSetFlag("disable-completely", new BlockableItemFlag(null));

	public final static SetFlag<String> DISABLE_THROW = new CustomSetFlag("disable-throw", new ThrowableItemFlag(null));
	
	public final static SetFlag<String> PERMIT_WORKBENCHES = new CustomSetFlag("permit-workbenches", new PermitWorkbenchesFlag(null));

	public final static Flag<String> ENTRY_MIN_LEVEL = new PlaceholderLevelFlag("entry-min-level");
	public final static Flag<String> ENTRY_MAX_LEVEL = new PlaceholderLevelFlag("entry-max-level");
	public final static Flag<String> ENTRY_PERMISSION = new PermissionStateFlag("entry-permission");
	public final static Flag<String> ENTRY_DENY_PERMISSION = new PermissionStateFlag("entry-deny-permission");
	public final static Flag<String> PLAYER_COUNT_LIMIT = new IntegerFlag("player-count-limit");

	public final static StateFlag CHAMBERED_ENDERPEARL = new StateFlag("chambered-enderpearl", true);

	/**
	 * Controls whether lightning strikes deal damage to players in this region.
	 * Default is allow (true) — lightning deals damage normally.
	 * Set to deny to show the lightning visually but cancel the damage to players.
	 */
	public final static StateFlag LIGHTNING_DAMAGE = new StateFlag("lightning-damage", true);

	/**
	 * When set to false, hits with a Wind Burst mace in this region still deal damage,
	 * but the enchantment does not launch the attacker.
	 * Usage: /rg flag &lt;region&gt; allow-windburst false
	 */
	public final static BooleanFlag ALLOW_WIND_BURST = new BooleanFlag("allow-windburst");

	/**
	 * When set to false, dropped items in this region cannot be destroyed by lava, fire, cactus,
	 * explosions or any other damage.
	 * Usage: /rg flag &lt;region&gt; destroy-items false
	 */
	public final static BooleanFlag DESTROY_ITEMS = new BooleanFlag("destroy-items");

	/**
	 * Blocks spawning of mobs in the listed categories within the region.
	 * Accepted values (comma-separated): ALL_HOSTILE, ALL_NEUTRAL, ALL_PEACEFUL, ALL,
	 * or specific EntityType names (e.g. ZOMBIE, CREEPER) for per-mob control.
	 * ALL_HOSTILE covers every {@code Enemy} mob (bosses like WITHER and ENDER_DRAGON included);
	 * ALL_NEUTRAL covers mobs that only attack when provoked; ALL_PEACEFUL covers every other
	 * living mob (villagers, animals, fish, bats, allays, armor stands, ...). Use clear to reset.
	 * See https://tinsware.github.io/wiki/docs/games/minecraft/plugins/worldguard-extraflags-plus/deny-mobspawn for the exact per-category mob lists.
	 * Usage: /rg flag <region> deny-mobspawn ALL_HOSTILE
	 */
	public final static SetFlag<String> DENY_MOB_SPAWN = new CustomSetFlag("deny-mobspawn", new MobSpawnDenyCategoryFlag(null));

	/**
	 * Repeats a console command at a fixed interval while a player stays in the region.
	 * Each entry format: {@code <seconds> <command>}
	 * Interval must be between 1 and 60 seconds.
	 * Usage: /rg flag <region> console-command-repeat "20 give %player% diamond 1"
	 */
	public final static SetFlag<String> CONSOLE_COMMAND_REPEAT = new CustomSetFlag("console-command-repeat", new RepeatCommandEntryFlag(null));

	/**
	 * Repeats a console command at a fixed interval while a player stays in the region.
	 * Timer resets on region exit — no cooldown persistence across exits (hold-to-earn).
	 * Each entry format: {@code <seconds> <command>}
	 * Interval must be between 1 and 300 seconds.
	 * Usage: /rg flag <region> console-command-timer "60 give %player% diamond 1"
	 */
	public final static SetFlag<String> CONSOLE_COMMAND_TIMER = new CustomSetFlag("console-command-timer", new RepeatCommandEntryFlag(null));
}



