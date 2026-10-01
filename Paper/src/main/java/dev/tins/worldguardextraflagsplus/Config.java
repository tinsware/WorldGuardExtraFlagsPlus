package dev.tins.worldguardextraflagsplus;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.tins.worldguardextraflagsplus.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Configuration manager using ConfigLib.
 * Maintains static methods for backward compatibility.
 */
public class Config
{
	private static JavaPlugin plugin;
	private static PluginConfig config;
	private static Path configFile;
	
	// ConfigLib properties with kebab-case formatter
	private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
		.setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
		.header(PluginConfig.CONFIG_HEADER)
		.build();
	
	public static final String CHECK_ORDER_DEFAULT = "allow-first";
	public static final String WATERLOGGED_MATERIAL_CHECK_DEFAULT = "both";
	
	public static void initialize(JavaPlugin plugin)
	{
		Config.plugin = plugin;
		
		// Get WorldGuard plugin data folder
		File worldGuardDataFolder = plugin.getServer().getPluginManager().getPlugin("WorldGuard").getDataFolder();
		
		// Create config-wgefp.yml in WorldGuard folder
		configFile = worldGuardDataFolder.toPath().resolve("config-wgefp.yml");
		
		// Load config
		reloadConfig();
	}
	
	public static void reloadConfig()
	{
		reloadConfig(false);
	}

	private static void reloadConfig(boolean recoveryAttempted)
	{
		try
		{
			// Ensure WorldGuard folder exists
			if (!configFile.getParent().toFile().exists())
			{
				configFile.getParent().toFile().mkdirs();
			}

			WgefpYamlFileGuard.checkAndQuarantineOversize(configFile, plugin.getLogger());
			
			// Load or update config using ConfigLib
			config = YamlConfigurations.update(configFile, PluginConfig.class, PROPERTIES);

			logStartupInfo("Loaded config from: " + configFile.toAbsolutePath());
		}
		catch (de.exlll.configlib.ConfigurationException e)
		{
			if (!recoveryAttempted && WgefpYamlFileGuard.isRecoverableYamlError(e))
			{
				try
				{
					WgefpYamlFileGuard.quarantineCorrupt(configFile, plugin.getLogger());
					plugin.getLogger().warning("Auto-recovery: quarantined config-wgefp.yml and retrying with fresh defaults.");
					reloadConfig(true);
					return;
				}
				catch (Exception recoveryError)
				{
					plugin.getLogger().log(Level.WARNING, "Auto-recovery failed: " + recoveryError.getMessage(), recoveryError);
				}
			}

			String errorMsg = WgefpYamlFileGuard.buildYamlErrorMessage(e, "config-wgefp.yml");
			WgefpYamlFileGuard.logYamlLoadFailure(plugin.getLogger(), configFile, "config-wgefp.yml", errorMsg, recoveryAttempted);
			disablePluginWithDefaultConfig();
		}
		catch (Exception e)
		{
			if (!recoveryAttempted && WgefpYamlFileGuard.isRecoverableGenericError(e))
			{
				try
				{
					WgefpYamlFileGuard.quarantineCorrupt(configFile, plugin.getLogger());
					plugin.getLogger().warning("Auto-recovery: quarantined config-wgefp.yml and retrying with fresh defaults.");
					reloadConfig(true);
					return;
				}
				catch (Exception recoveryError)
				{
					plugin.getLogger().log(Level.WARNING, "Auto-recovery failed: " + recoveryError.getMessage(), recoveryError);
				}
			}

			String errorMsg = e.getMessage();
			if (errorMsg != null && errorMsg.length() > 100)
			{
				errorMsg = errorMsg.substring(0, 100) + "...";
			}
			WgefpYamlFileGuard.logYamlLoadFailure(plugin.getLogger(), configFile, "config-wgefp.yml",
					"Failed to load config-wgefp.yml: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName()),
					recoveryAttempted);
			disablePluginWithDefaultConfig();
		}
	}

	private static void disablePluginWithDefaultConfig()
	{
		plugin.getServer().getPluginManager().disablePlugin(plugin);
		config = new PluginConfig();
	}
	
	// Static getter methods for backward compatibility
	public static boolean isPermitWorkbenchBlockPlacementToo()
	{
		return config != null ? config.getPermitWorkbenches().isPermitWorkbenchBlockPlacementToo() : false;
	}
	
	public static boolean isPermitAllIncludesEnderchest()
	{
		return config != null ? config.getPermitWorkbenches().isPermitAllIncludesEnderchest() : false;
	}
	
	public static boolean isAutoGiveGodmodeRegionLeft()
	{
		return config != null ? config.getGodmode().isAutoGiveGodmodeRegionLeft() : false;
	}

	public static boolean isVerboseStartupLogs()
	{
		return config != null && config.getLogging() != null && config.getLogging().isVerboseStartupLogs();
	}

	public static void logStartupInfo(String message)
	{
		if (plugin != null && isVerboseStartupLogs())
		{
			plugin.getLogger().info(message);
		}
	}

	public static void logStartupFine(String message)
	{
		if (plugin != null)
		{
			plugin.getLogger().fine(message);
		}
	}


	// Flag control methods
	public static boolean isFlagEnabled(String flagName)
	{
		if (config == null || config.getAllFlagsControl() == null)
		{
			return true; // Default to enabled for backward compatibility
		}

		PluginConfig.AllFlagsControl flags = config.getAllFlagsControl();

		switch (flagName.toLowerCase())
		{
			// Location & Teleportation
			case "teleport-on-entry": return flags.isTeleportOnEntry();
			case "teleport-on-exit": return flags.isTeleportOnExit();
			case "teleport-on-exit-ignore-teleports": return flags.isTeleportOnExitIgnoreTeleports();
			case "join-location": return flags.isJoinLocation();
			case "respawn-location": return flags.isRespawnLocation();

			// Command Execution
			case "command-on-entry": return flags.isCommandOnEntry();
			case "command-on-exit": return flags.isCommandOnExit();
			case "console-command-on-entry": return flags.isConsoleCommandOnEntry();
			case "console-command-on-exit": return flags.isConsoleCommandOnExit();

			// Movement & Speed Control
			case "walk-speed": return flags.isWalkSpeed();
			case "fly-speed": return flags.isFlySpeed();
			case "fly": return flags.isFly();
			case "glide": return flags.isGlide();
			case "frostwalker": return flags.isFrostwalker();

			// Protection & Survival
			case "godmode": return flags.isGodmode();
			case "keep-inventory": return flags.isKeepInventory();
			case "keep-exp": return flags.isKeepExp();
			case "item-durability": return flags.isItemDurability();

			// Chat Modification
			case "chat-prefix": return flags.isChatPrefix();
			case "chat-suffix": return flags.isChatSuffix();

			// Effect Control
			case "blocked-effects": return flags.isBlockedEffects();
			case "give-effects": return flags.isGiveEffects();

			// World Interaction
			case "worldedit": return flags.isWorldedit();
			case "play-sounds": return flags.isPlaySounds();
			case "nether-portals": return flags.isNetherPortals();
			case "chunk-unload": return flags.isChunkUnload();
			case "villager-trade": return flags.isVillagerTrade();
			case "enderchest-access": return flags.isEnderchestAccess();
			case "inventory-craft": return flags.isInventoryCraft();

			// Block & Item Control
			case "allow-block-place": return flags.isAllowBlockPlace();
			case "deny-block-place": return flags.isDenyBlockPlace();
			case "allow-block-break": return flags.isAllowBlockBreak();
			case "deny-block-break": return flags.isDenyBlockBreak();
			case "deny-item-drops": return flags.isDenyItemDrops();
			case "deny-item-pickup": return flags.isDenyItemPickup();
			case "disable-completely": return flags.isDisableCompletely();
			case "disable-throw": return flags.isDisableThrow();
			case "permit-workbenches": return flags.isPermitWorkbenches();

			// Entry Control
			case "entry-min-level": return flags.isEntryMinLevel();
			case "entry-max-level": return flags.isEntryMaxLevel();
			case "entry-permission": return flags.isEntryPermission();
			case "entry-deny-permission": return flags.isEntryDenyPermission();
			case "player-count-limit": return flags.isPlayerCountLimit();

			// Special Features
			case "disable-collision": return flags.isDisableCollision();
			case "chambered-enderpearl": return flags.isChamberedEnderPearl();
			case "hide-players": return flags.isHidePlayers();
			case "lightning-damage": return flags.isLightningDamage();
			case "deny-mobspawn": return flags.isDenyMobSpawn();
			case "console-command-repeat": return flags.isConsoleCommandRepeat();
			case "console-command-timer": return flags.isConsoleCommandTimer();

			// PlaceholderAPI
			case "papi-placeholders": return flags.isPapiPlaceholders();

			default: return true; // Default to enabled for unknown flags
		}
	}

	public static File getConfigFile()
	{
		return configFile != null ? configFile.toFile() : null;
	}

	public static boolean isCombatLogKeepInventoryEnabled()
	{
		return config != null
				&& config.getKeepInventorySettings() != null
				&& config.getKeepInventorySettings().isCombatLogRestore();
	}

	public static boolean isAllowBlockPlaceRequireMembership()
	{
		return config != null
				&& config.getAllowBlockPlaceSettings() != null
				&& config.getAllowBlockPlaceSettings().isRequireMembership();
	}

	public static boolean isAllowBlockBreakRequireMembership()
	{
		return config != null
				&& config.getAllowBlockBreakSettings() != null
				&& config.getAllowBlockBreakSettings().isRequireMembership();
	}

	public static String getCheckOrder()
	{
		return config != null && config.getAllFlagsControl() != null && config.getAllFlagsControl().getCheckOrder() != null
				? config.getAllFlagsControl().getCheckOrder()
				: CHECK_ORDER_DEFAULT;
	}

	public static boolean isDenyFirst()
	{
		return "deny-first".equalsIgnoreCase(getCheckOrder());
	}

	public static String getWaterloggedMaterialCheck()
	{
		if (config == null || config.getAllowBlockBreakSettings() == null)
		{
			return WATERLOGGED_MATERIAL_CHECK_DEFAULT;
		}

		String value = config.getAllowBlockBreakSettings().getWaterloggedMaterialCheck();
		return value != null ? value : WATERLOGGED_MATERIAL_CHECK_DEFAULT;
	}
}
