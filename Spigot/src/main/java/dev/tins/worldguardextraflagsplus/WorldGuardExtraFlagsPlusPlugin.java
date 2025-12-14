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

		try
		{
			FlagRegistry flagRegistry = this.worldGuard.getFlagRegistry();
			flagRegistry.register(Flags.TELEPORT_ON_ENTRY);
			flagRegistry.register(Flags.TELEPORT_ON_EXIT);
			flagRegistry.register(Flags.COMMAND_ON_ENTRY);
			flagRegistry.register(Flags.COMMAND_ON_EXIT);
			flagRegistry.register(Flags.CONSOLE_COMMAND_ON_ENTRY);
			flagRegistry.register(Flags.CONSOLE_COMMAND_ON_EXIT);
			flagRegistry.register(Flags.WALK_SPEED);
			flagRegistry.register(Flags.KEEP_INVENTORY);
			flagRegistry.register(Flags.KEEP_EXP);
			flagRegistry.register(Flags.CHAT_PREFIX);
			flagRegistry.register(Flags.CHAT_SUFFIX);
			flagRegistry.register(Flags.BLOCKED_EFFECTS);
			flagRegistry.register(Flags.GODMODE);
			flagRegistry.register(Flags.RESPAWN_LOCATION);
			flagRegistry.register(Flags.WORLDEDIT);
			flagRegistry.register(Flags.GIVE_EFFECTS);
			flagRegistry.register(Flags.FLY);
			flagRegistry.register(Flags.FLY_SPEED);
			flagRegistry.register(Flags.PLAY_SOUNDS);
			flagRegistry.register(Flags.FROSTWALKER);
			flagRegistry.register(Flags.NETHER_PORTALS);
			flagRegistry.register(Flags.ALLOW_BLOCK_PLACE);
			flagRegistry.register(Flags.DENY_BLOCK_PLACE);
			flagRegistry.register(Flags.ALLOW_BLOCK_BREAK);
			flagRegistry.register(Flags.DENY_BLOCK_BREAK);
			flagRegistry.register(Flags.DENY_ITEM_DROPS);
			flagRegistry.register(Flags.DENY_ITEM_PICKUP);
			flagRegistry.register(Flags.GLIDE);
			flagRegistry.register(Flags.CHUNK_UNLOAD);
      flagRegistry.register(Flags.ITEM_DURABILITY);
      flagRegistry.register(Flags.JOIN_LOCATION);
      
      flagRegistry.register(Flags.DISABLE_COMPLETELY);
      flagRegistry.register(Flags.PERMIT_WORKBENCHES);
      flagRegistry.register(Flags.ENTRY_MIN_LEVEL);
      flagRegistry.register(Flags.ENTRY_MAX_LEVEL);
      flagRegistry.register(Flags.VILLAGER_TRADE);
      flagRegistry.register(Flags.INVENTORY_CRAFT);
      flagRegistry.register(Flags.DISABLE_COLLISION);
		}
		catch (Exception e)
		{
			this.getServer().getPluginManager().disablePlugin(this);

			throw new RuntimeException(e instanceof IllegalStateException ?
					"WorldGuard prevented flag registration. Did you reload the plugin? This is not supported!" :
					"Flag registration failed!", e);
		}
		
		try
		{
			Plugin protocolLibPlugin = this.getServer().getPluginManager().getPlugin("ProtocolLib");
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
		
		// Note: Collision team initialization is done lazily on first use
		// because Folia doesn't allow team registration on main scoreboard during startup
		
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

		this.sessionManager.registerHandler(TeleportOnEntryFlagHandler.FACTORY(plugin), null);
		this.sessionManager.registerHandler(TeleportOnExitFlagHandler.FACTORY(plugin), null);

		this.sessionManager.registerHandler(WalkSpeedFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(FlySpeedFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(FlyFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(GlideFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(GodmodeFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(PlaySoundsFlagHandler.FACTORY(plugin), null);
		this.sessionManager.registerHandler(BlockedEffectsFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(GiveEffectsFlagHandler.FACTORY(), null);

		this.sessionManager.registerHandler(CommandOnEntryFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(CommandOnExitFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(ConsoleCommandOnEntryFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(ConsoleCommandOnExitFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(EntryLevelFlagHandler.FACTORY(plugin), null);
		this.sessionManager.registerHandler(CollisionFlagHandler.FACTORY(), null);

		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		this.getServer().getPluginManager().registerEvents(new BlockListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		this.getServer().getPluginManager().registerEvents(new WorldListener(this, this.regionContainer), this);
		this.getServer().getPluginManager().registerEvents(new EntityListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		this.getServer().getPluginManager().registerEvents(new VillagerTradeListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		this.getServer().getPluginManager().registerEvents(new dev.tins.worldguardextraflagsplus.listeners.WorldGuardReloadListener(this), this);

		this.worldEditPlugin.getWorldEdit().getEventBus().register(new WorldEditListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager));
		
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



