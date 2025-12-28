package dev.tins.worldguardextraflagsplus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import dev.tins.worldguardextraflagsplus.listeners.*;
import dev.tins.worldguardextraflagsplus.wg.handlers.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;

import lombok.Getter;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.protocollib.ProtocolLibHelper;
import dev.tins.worldguardextraflagsplus.updater.UpdateChecker;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardExtraFlagsPlusPlugin extends JavaPlugin
{
	private static final Set<Flag<?>> FLAGS = WorldGuardExtraFlagsPlusPlugin.getPluginFlags();
	@Getter private static WorldGuardExtraFlagsPlusPlugin plugin;

	@Getter private WorldEditPlugin worldEditPlugin;

	@Getter private WorldGuardPlugin worldGuardPlugin;
	@Getter private WorldGuard worldGuard;

	@Getter private RegionContainer regionContainer;
	@Getter private SessionManager sessionManager;

	@Getter private ProtocolLibHelper protocolLibHelper;
	
	// Collision flag handler (uses native Minecraft teams, no external libraries needed)
	@Getter private boolean collisionFlagEnabled = false;
	@Getter private dev.tins.worldguardextraflagsplus.collision.CollisionPacketHandler collisionPacketHandler = null;
	
	public WorldGuardExtraFlagsPlusPlugin()
	{
		WorldGuardExtraFlagsPlusPlugin.plugin = this;
	}
	
	@Override
	public void onLoad()
	{
		this.worldEditPlugin = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
		this.worldGuardPlugin = (WorldGuardPlugin) this.getServer().getPluginManager().getPlugin("WorldGuard");

		this.worldGuard = WorldGuard.getInstance();
		
		// Collision flag uses native Minecraft teams - no external libraries needed
		// Enable by default; scoreboard availability will be checked in onEnable() when server is fully loaded
		this.collisionFlagEnabled = true;
		
		// Get ProtocolLib plugin for give-effects flag (separate from collision)
		Plugin protocolLibPlugin = this.getServer().getPluginManager().getPlugin("ProtocolLib");

		try
		{
			FlagRegistry flagRegistry = this.worldGuard.getFlagRegistry();

			// Register flags based on configuration
			if (Config.isFlagEnabled("teleport-on-entry")) flagRegistry.register(Flags.TELEPORT_ON_ENTRY);
			if (Config.isFlagEnabled("teleport-on-exit")) flagRegistry.register(Flags.TELEPORT_ON_EXIT);
			if (Config.isFlagEnabled("command-on-entry")) flagRegistry.register(Flags.COMMAND_ON_ENTRY);
			if (Config.isFlagEnabled("command-on-exit")) flagRegistry.register(Flags.COMMAND_ON_EXIT);
			if (Config.isFlagEnabled("console-command-on-entry")) flagRegistry.register(Flags.CONSOLE_COMMAND_ON_ENTRY);
			if (Config.isFlagEnabled("console-command-on-exit")) flagRegistry.register(Flags.CONSOLE_COMMAND_ON_EXIT);
			if (Config.isFlagEnabled("walk-speed")) flagRegistry.register(Flags.WALK_SPEED);
			if (Config.isFlagEnabled("keep-inventory")) flagRegistry.register(Flags.KEEP_INVENTORY);
			if (Config.isFlagEnabled("keep-exp")) flagRegistry.register(Flags.KEEP_EXP);
			if (Config.isFlagEnabled("chat-prefix")) flagRegistry.register(Flags.CHAT_PREFIX);
			if (Config.isFlagEnabled("chat-suffix")) flagRegistry.register(Flags.CHAT_SUFFIX);
			if (Config.isFlagEnabled("blocked-effects")) flagRegistry.register(Flags.BLOCKED_EFFECTS);
			if (Config.isFlagEnabled("godmode")) flagRegistry.register(Flags.GODMODE);
			if (Config.isFlagEnabled("respawn-location")) flagRegistry.register(Flags.RESPAWN_LOCATION);
			if (Config.isFlagEnabled("worldedit")) flagRegistry.register(Flags.WORLDEDIT);
			if (Config.isFlagEnabled("give-effects")) flagRegistry.register(Flags.GIVE_EFFECTS);
			if (Config.isFlagEnabled("fly")) flagRegistry.register(Flags.FLY);
			if (Config.isFlagEnabled("fly-speed")) flagRegistry.register(Flags.FLY_SPEED);
			if (Config.isFlagEnabled("play-sounds")) flagRegistry.register(Flags.PLAY_SOUNDS);
			if (Config.isFlagEnabled("frostwalker")) flagRegistry.register(Flags.FROSTWALKER);
			if (Config.isFlagEnabled("nether-portals")) flagRegistry.register(Flags.NETHER_PORTALS);
			if (Config.isFlagEnabled("allow-block-place")) flagRegistry.register(Flags.ALLOW_BLOCK_PLACE);
			if (Config.isFlagEnabled("deny-block-place")) flagRegistry.register(Flags.DENY_BLOCK_PLACE);
			if (Config.isFlagEnabled("allow-block-break")) flagRegistry.register(Flags.ALLOW_BLOCK_BREAK);
			if (Config.isFlagEnabled("deny-block-break")) flagRegistry.register(Flags.DENY_BLOCK_BREAK);
			if (Config.isFlagEnabled("deny-item-drops")) flagRegistry.register(Flags.DENY_ITEM_DROPS);
			if (Config.isFlagEnabled("deny-item-pickup")) flagRegistry.register(Flags.DENY_ITEM_PICKUP);
			if (Config.isFlagEnabled("glide")) flagRegistry.register(Flags.GLIDE);
			if (Config.isFlagEnabled("chunk-unload")) flagRegistry.register(Flags.CHUNK_UNLOAD);
			if (Config.isFlagEnabled("item-durability")) flagRegistry.register(Flags.ITEM_DURABILITY);
			if (Config.isFlagEnabled("join-location")) flagRegistry.register(Flags.JOIN_LOCATION);
			if (Config.isFlagEnabled("disable-completely")) flagRegistry.register(Flags.DISABLE_COMPLETELY);
			if (Config.isFlagEnabled("permit-workbenches")) flagRegistry.register(Flags.PERMIT_WORKBENCHES);
			if (Config.isFlagEnabled("entry-min-level")) flagRegistry.register(Flags.ENTRY_MIN_LEVEL);
			if (Config.isFlagEnabled("entry-max-level")) flagRegistry.register(Flags.ENTRY_MAX_LEVEL);
			if (Config.isFlagEnabled("villager-trade")) flagRegistry.register(Flags.VILLAGER_TRADE);
			if (Config.isFlagEnabled("inventory-craft")) flagRegistry.register(Flags.INVENTORY_CRAFT);

			// Register collision flag (scoreboard availability will be checked in onEnable())
			if (Config.isFlagEnabled("disable-collision")) flagRegistry.register(Flags.DISABLE_COLLISION);
		}
		catch (Exception e)
		{
			this.getServer().getPluginManager().disablePlugin(this);

			throw new RuntimeException(e instanceof IllegalStateException ?
					"WorldGuard prevented flag registration. Did you reload the plugin? This is not supported!" :
					"Flag registration failed!", e);
		}
		
		// Initialize ProtocolLib helper (for potion effect packet suppression)
		// This is separate from collision flag - ProtocolLib is used for give-effects flag
		try
		{
			if (protocolLibPlugin != null)
			{
				this.protocolLibHelper = new ProtocolLibHelper(this, protocolLibPlugin);
			}
		}
		catch(Throwable ignore)
		{
		}
	}
	
	@Override
	public void onEnable()
	{
		// Initialize messages system first (loads messages-wgefp.yml from WorldGuard folder)
		Messages.initialize(this);
		
		// Initialize config system (loads config-wgefp.yml from WorldGuard folder)
		Config.initialize(this);
		
		// Migrate region files: rename permit-completely to disable-completely (file only, in-memory update happens later)
		java.util.Set<String> migratedWorlds = this.migrateRegionFiles();
		
		WorldGuardUtils.initializeScheduler(this);
		
		// Clean up old collision team from previous team-based implementation
		// Run after server fully loads to ensure scoreboard is available
		WorldGuardUtils.getScheduler().runNextTick(task -> {
			this.cleanupOldCollisionTeam();
		});
		
		this.regionContainer = this.worldGuard.getPlatform().getRegionContainer();
		this.sessionManager = this.worldGuard.getPlatform().getSessionManager();
		
		// Now update in-memory region objects for worlds that were migrated
		if (!migratedWorlds.isEmpty())
		{
			for (String worldName : migratedWorlds)
			{
				this.updateInMemoryRegions(worldName);
			}
		}

		// Register handlers based on configuration
		if (Config.isFlagEnabled("teleport-on-entry")) this.sessionManager.registerHandler(TeleportOnEntryFlagHandler.FACTORY(plugin), null);
		if (Config.isFlagEnabled("teleport-on-exit")) this.sessionManager.registerHandler(TeleportOnExitFlagHandler.FACTORY(plugin), null);

		if (Config.isFlagEnabled("walk-speed")) this.sessionManager.registerHandler(WalkSpeedFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("fly-speed")) this.sessionManager.registerHandler(FlySpeedFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("fly")) this.sessionManager.registerHandler(FlyFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("glide")) this.sessionManager.registerHandler(GlideFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("godmode")) this.sessionManager.registerHandler(GodmodeFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("play-sounds")) this.sessionManager.registerHandler(PlaySoundsFlagHandler.FACTORY(plugin), null);
		if (Config.isFlagEnabled("blocked-effects")) this.sessionManager.registerHandler(BlockedEffectsFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("give-effects")) this.sessionManager.registerHandler(GiveEffectsFlagHandler.FACTORY(), null);

		if (Config.isFlagEnabled("command-on-entry")) this.sessionManager.registerHandler(CommandOnEntryFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("command-on-exit")) this.sessionManager.registerHandler(CommandOnExitFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("console-command-on-entry")) this.sessionManager.registerHandler(ConsoleCommandOnEntryFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("console-command-on-exit")) this.sessionManager.registerHandler(ConsoleCommandOnExitFlagHandler.FACTORY(), null);
		if (Config.isFlagEnabled("entry-min-level") || Config.isFlagEnabled("entry-max-level")) this.sessionManager.registerHandler(EntryLevelFlagHandler.FACTORY(plugin), null);
		
		// Initialize collision handler (uses native Minecraft teams, no external libraries needed)
		// Check scoreboard availability now that server is fully loaded
		try
		{
			// Check if TAB is installed (TAB integration is supported)
			boolean tabInstalled = this.getServer().getPluginManager().getPlugin("TAB") != null;
			
      this.getLogger().info(" ");
			// Show info about team conflicts
			this.getLogger().info("[Collision Flag] The disable-collision flag uses Minecraft teams.");
			this.getLogger().info("[Collision Flag] Using this flag with plugins that manage teams may cause conflicts!");
			this.getLogger().info("[Collision Flag] Do NOT use the disable-collision flag if you have these plugins installed.");
      // Show supported plugins with team integration
			this.getLogger().info("[Collision Flag] Supported plugins with (1): TAB");
      this.getLogger().info(" ");

			
			if (this.getServer().getScoreboardManager() == null || 
			    this.getServer().getScoreboardManager().getMainScoreboard() == null)
			{
				this.collisionFlagEnabled = false;
				this.getLogger().warning("[Collision Flag] Scoreboard not available - collision feature will be disabled");
			}
			else
			{
				// Scoreboard is available, initialize the collision handler
				try
				{
					this.collisionPacketHandler = new dev.tins.worldguardextraflagsplus.collision.TeamCollisionHandler(this);
					
					// Initialize the collision handler
					if (!this.collisionPacketHandler.initialize())
					{
						this.getLogger().warning("[Collision Flag] Failed to initialize collision handler");
						this.getLogger().warning("[Collision Flag] Collision feature will be disabled");
						this.collisionFlagEnabled = false;
						this.collisionPacketHandler = null;
					}
					else
					{
						this.getLogger().info("[Collision Flag] Initialized " + this.collisionPacketHandler.getLibraryName() + " collision handler");
						
						// Register collision handler if initialized successfully
						this.sessionManager.registerHandler(CollisionFlagHandler.FACTORY(), null);
					}
				}
				catch (Throwable e)
				{
					this.getLogger().warning("[Collision Flag] Failed to register collision handler: " + 
						(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
					this.getLogger().warning("[Collision Flag] Collision feature will be disabled");
					this.collisionFlagEnabled = false;
					if (this.collisionPacketHandler != null)
					{
						this.collisionPacketHandler.cleanup();
						this.collisionPacketHandler = null;
					}
				}
			}
		}
		catch (Exception e)
		{
			this.collisionFlagEnabled = false;
			this.getLogger().warning("[Collision Flag] Failed to check scoreboard availability: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			this.getLogger().warning("[Collision Flag] Collision feature will be disabled");
		}

		// Register PlayerListener (contains multiple event handlers)
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);

		// Conditionally register JoinLocationListener only if join-location flag is enabled
		if (Config.isFlagEnabled("join-location"))
		{
			this.getServer().getPluginManager().registerEvents(new JoinLocationListener(this.worldGuardPlugin, this.regionContainer), this);
			this.sendJoinLocationDeprecationWarning();
		} else {
      this.getLogger().info(" ");
			this.getLogger().info("[Join Location Flag] Disabled in config-wgefp.yml it will not load the flag");
      this.getLogger().info(" ");
		}
		// Register listeners based on configuration
		if (Config.isFlagEnabled("allow-block-place") || Config.isFlagEnabled("deny-block-place") ||
		    Config.isFlagEnabled("allow-block-break") || Config.isFlagEnabled("deny-block-break") ||
		    Config.isFlagEnabled("frostwalker"))
		{
			this.getServer().getPluginManager().registerEvents(new BlockListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		}

		if (Config.isFlagEnabled("chunk-unload") || Config.isFlagEnabled("nether-portals"))
		{
			this.getServer().getPluginManager().registerEvents(new WorldListener(this, this.regionContainer), this);
		}

		// EntityListener handles multiple flags
		if (Config.isFlagEnabled("disable-completely") || Config.isFlagEnabled("permit-workbenches") ||
		    Config.isFlagEnabled("inventory-craft") || Config.isFlagEnabled("nether-portals") ||
		    Config.isFlagEnabled("allow-block-break") || Config.isFlagEnabled("allow-block-place") ||
		    Config.isFlagEnabled("glide"))
		{
			this.getServer().getPluginManager().registerEvents(new EntityListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		}

		if (Config.isFlagEnabled("villager-trade"))
		{
			this.getServer().getPluginManager().registerEvents(new VillagerTradeListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		}

		this.getServer().getPluginManager().registerEvents(new dev.tins.worldguardextraflagsplus.listeners.WorldGuardReloadListener(this), this);

		if (Config.isFlagEnabled("worldedit"))
		{
			this.worldEditPlugin.getWorldEdit().getEventBus().register(new WorldEditListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager));
		}
		
		if (this.protocolLibHelper != null)
		{
			try
			{
				this.protocolLibHelper.onEnable();
			}
			catch (Throwable ignore)
			{
				this.getServer().getPluginManager().registerEvents(new EntityPotionEffectEventListener(this.worldGuardPlugin, this.sessionManager), this);
			}
		}
		else
		{
			this.getServer().getPluginManager().registerEvents(new EntityPotionEffectEventListener(this.worldGuardPlugin, this.sessionManager), this);
		}
		
		for(World world : this.getServer().getWorlds())
		{
			this.doUnloadChunkFlagCheck(world);
		}

		this.setupMetrics();
		
		// Setup update checker
		this.setupUpdateChecker();
		
		// Register reload command
		this.getCommand("wgefp").setExecutor(new dev.tins.worldguardextraflagsplus.commands.ReloadCommand(this));
		this.getCommand("wgefp").setTabCompleter(new dev.tins.worldguardextraflagsplus.commands.ReloadCommand(this));
	}

  private void sendJoinLocationDeprecationWarning()
  {
    this.getLogger().info(" >> >> ");
    this.getLogger().info(" >> >> If you are not using \"join-location\" flag, you can disable it in the \"config-wgefp.yml\" ");
    this.getLogger().info(" >> >> This will avoid the deprecation warning on server load");
    this.getLogger().info(" >> >> Otherwise, the plugin will work correctly, deprecation warning will not broke working of it.");
		this.getLogger().info(" >> >> ");
  }

	public void doUnloadChunkFlagCheck(org.bukkit.World world)
	{
		RegionManager regionManager = this.regionContainer.get(BukkitAdapter.adapt(world));
		if (regionManager == null)
		{
			return;
		}

		for (ProtectedRegion region : regionManager.getRegions().values())
		{
			if (region.getFlag(Flags.CHUNK_UNLOAD) == StateFlag.State.DENY)
			{
				this.getLogger().info("Loading chunks for region " + region.getId() + " located in " + world.getName() + " due to chunk-unload flag being deny");

				BlockVector3 min = region.getMinimumPoint();
				BlockVector3 max = region.getMaximumPoint();

				for(int x = min.getBlockX() >> 4; x <= max.getBlockX() >> 4; x++)
				{
					for(int z = min.getBlockZ() >> 4; z <= max.getBlockZ() >> 4; z++)
					{
						world.getChunkAt(x, z).addPluginChunkTicket(this);
					}
				}
			}
		}
	}
	
	/**
	 * Migrates region files: renames "permit-completely" flag to "disable-completely" in region YAML files.
	 * This is a one-time migration that runs on plugin enable.
	 * Returns a set of world names that were migrated (for in-memory update later).
	 */
	private java.util.Set<String> migrateRegionFiles()
	{
		java.util.Set<String> migratedWorlds = new java.util.HashSet<>();
		
		try
		{
			File worldGuardDataFolder = this.worldGuardPlugin.getDataFolder();
			File worldsFolder = new File(worldGuardDataFolder, "worlds");
			
			if (!worldsFolder.exists() || !worldsFolder.isDirectory())
			{
				return migratedWorlds; // No worlds folder, nothing to migrate
			}
			
			int migratedCount = 0;
			
			// Iterate through all world folders
			File[] worldFolders = worldsFolder.listFiles(File::isDirectory);
			if (worldFolders == null)
			{
				return migratedWorlds;
			}
			
			for (File worldFolder : worldFolders)
			{
				File regionsFile = new File(worldFolder, "regions.yml");
				if (!regionsFile.exists() || !regionsFile.isFile())
				{
					continue; // No regions file for this world
				}
				
				try
				{
					// Read file as text to preserve exact formatting
					String fileContent = new String(Files.readAllBytes(regionsFile.toPath()), StandardCharsets.UTF_8);
					String originalContent = fileContent;
					
					// Use regex to replace permit-completely with disable-completely
					// Match: permit-completely: (with any whitespace before it, preserving indentation)
					// This preserves all formatting, spacing, and structure
					fileContent = fileContent.replaceAll("(?m)^(\\s*)permit-completely\\s*:", "$1disable-completely:");
					
					// Only write if content changed
					if (!fileContent.equals(originalContent))
					{
						// Write back with exact same formatting
						Files.write(regionsFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
						
						// Count how many regions were migrated (count occurrences)
						int occurrences = (originalContent.length() - originalContent.replace("permit-completely:", "").length()) / "permit-completely:".length();
						migratedCount += occurrences;
						
						this.getLogger().info("Migrated flag 'permit-completely' to 'disable-completely' in region file for world '" + worldFolder.getName() + "' (" + occurrences + " occurrence(s))");
						
						// Add to set for in-memory update later
						migratedWorlds.add(worldFolder.getName());
					}
				}
				catch (Exception e)
				{
					this.getLogger().log(java.util.logging.Level.WARNING, "Failed to migrate region file for world '" + worldFolder.getName() + "': " + e.getMessage(), e);
				}
			}
			
			if (migratedCount > 0)
			{
				this.getLogger().info("Region file migration completed: " + migratedCount + " flag occurrence(s) migrated from 'permit-completely' to 'disable-completely'");
			}
		}
		catch (Exception e)
		{
			this.getLogger().log(java.util.logging.Level.WARNING, "Failed to migrate region files: " + e.getMessage(), e);
		}
		
		return migratedWorlds;
	}
	
	/**
	 * Updates in-memory region objects to migrate permit-completely flag to disable-completely.
	 * This prevents the flag from reverting when WorldGuard saves regions.
	 * Note: Since permit-completely flag is removed, this method only handles file-based migration.
	 * In-memory regions will be updated when WorldGuard reloads regions from files.
	 */
	private void updateInMemoryRegions(String worldName)
	{
		// Note: permit-completely flag has been removed.
		// File-based migration handles the conversion in regions.yml files.
		// When WorldGuard reloads regions, it will read the migrated disable-completely flag from files.
		// This method is kept for compatibility but no longer performs in-memory migration.
	}
	
	/**
	 * Cleans up the old collision team from the previous team-based implementation.
	 * This method removes the WGEFP_COLLISION_DISABLED team and all its entries.
	 * Called once after server load to clean up legacy team-based collision system.
	 */
	private void cleanupOldCollisionTeam()
	{
		try
		{
			org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
			if (scoreboard == null)
			{
				return;
			}
			
			org.bukkit.scoreboard.Team team = scoreboard.getTeam("WGEFP_COLLISION_DISABLED");
			if (team != null)
			{
				// Remove all entries from the team first
				java.util.Set<String> entries = new java.util.HashSet<>(team.getEntries());
				for (String entry : entries)
				{
					try
					{
						team.removeEntry(entry);
					}
					catch (Exception e)
					{
						// Ignore errors when removing entries
					}
				}
				
				// Unregister the team
				try
				{
					team.unregister();
					this.getLogger().info("[Collision Flag] Cleaned up old collision team from previous implementation");
				}
				catch (Exception e)
				{
					// Team might already be unregistered, ignore
				}
			}
		}
		catch (Exception e)
		{
			// Silently fail - team cleanup is not critical
			this.getLogger().fine("[Collision Flag] Could not clean up old collision team: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
		}
	}

  
	
	private void setupMetrics()
	{
		final int bStatsPluginId = 27821;
		
		try
		{
			Metrics metrics = new Metrics(this, bStatsPluginId);
			this.getLogger().info("bStats metrics enabled (ID: " + bStatsPluginId + ")");
			
			// Note: Custom charts can be added here once chart types are confirmed
			// Track flag usage statistics
		}
		catch (Exception e)
		{
			this.getLogger().warning("Failed to initialize bStats metrics: " + e.getMessage());
		}
	}
	
	private void setupUpdateChecker()
	{
		// Hardcoded values
		// Spigot resource ID: https://www.spigotmc.org/resources/worldguard-extraflags-plus.129946/
		int spigotResourceId = 129946;
		
		// GitHub repository
		String githubRepository = "tins-dev/WorldGuardExtraFlagsPlus";
		
		// Modrinth project ID: https://modrinth.com/plugin/worldguard-extraflags-plus
		String modrinthProjectId = "worldguard-extraflags-plus";
		
		// Run update checker after server fully loads (after "Done" message)
		// Use FoliaLib scheduler for Folia compatibility
		// Use a delayed task to ensure server is fully loaded (5 seconds delay)
		WorldGuardUtils.getScheduler().runNextTick(task -> {
			// Additional delay to ensure server is fully loaded
			WorldGuardUtils.getScheduler().runNextTick(delayedTask -> {
				UpdateChecker updateChecker = new UpdateChecker(this, spigotResourceId, githubRepository, modrinthProjectId);
				updateChecker.checkForUpdates();
			});
		});
	}
	
	@Override
	public void onDisable()
	{
		// Cleanup collision handler
		if (this.collisionPacketHandler != null)
		{
			try
			{
				this.collisionPacketHandler.cleanup();
			}
			catch (Exception e)
			{
				this.getLogger().warning("[Collision Flag] Error during cleanup: " + 
					(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			}
		}
	}
	
	private static Set<Flag<?>> getPluginFlags()
	{
		Set<Flag<?>> flags = new HashSet<>();
		
		for (Field field : Flags.class.getFields())
		{
			try
			{
				flags.add((Flag<?>)field.get(null));
			}
			catch (IllegalArgumentException | IllegalAccessException ignored)
			{
			}
		}
		
		return flags;
	}
}



