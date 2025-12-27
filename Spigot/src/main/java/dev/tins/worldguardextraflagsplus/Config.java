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
		try
		{
			// Ensure WorldGuard folder exists
			if (!configFile.getParent().toFile().exists())
			{
				configFile.getParent().toFile().mkdirs();
			}
			
			// Load or update config using ConfigLib
			config = YamlConfigurations.update(configFile, PluginConfig.class, PROPERTIES);
			
			plugin.getLogger().info("Loaded config from: " + configFile.toAbsolutePath());
		}
		catch (de.exlll.configlib.ConfigurationException e)
		{
			// Extract root cause for better error message
			Throwable cause = e.getCause();
			String errorMsg = "Invalid YAML in config-wgefp.yml";
			
			// Check if it's a duplicate key exception (using class name since it's shaded)
			if (cause != null && cause.getClass().getSimpleName().equals("DuplicateKeyException"))
			{
				String message = cause.getMessage();
				if (message != null && message.contains("duplicate key"))
				{
					int keyStart = message.indexOf("duplicate key");
					if (keyStart != -1)
					{
						String keyPart = message.substring(keyStart);
						errorMsg = "Duplicate key found in config-wgefp.yml: " + keyPart.split("\n")[0].replace("found duplicate key", "").trim();
					}
					else
					{
						errorMsg = "Duplicate key found in config-wgefp.yml. Check the file for duplicate entries.";
					}
				}
				else
				{
					errorMsg = "Duplicate key found in config-wgefp.yml. Check the file for duplicate entries.";
				}
			}
			else if (cause != null)
			{
				String causeMsg = cause.getMessage();
				if (causeMsg != null && causeMsg.contains("duplicate key"))
				{
					errorMsg = "Duplicate key found in config-wgefp.yml. Check the file for duplicate entries.";
				}
				else
				{
					errorMsg = causeMsg != null ? causeMsg : cause.getClass().getSimpleName();
					if (errorMsg.length() > 100)
					{
						errorMsg = errorMsg.substring(0, 100) + "...";
					}
				}
			}
			
			plugin.getLogger().severe("CRITICAL: " + errorMsg);
			plugin.getLogger().severe("File location: " + configFile.toAbsolutePath());
			plugin.getLogger().severe("Disabling plugin. Please fix the YAML file and restart the server.");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			// Use default config as fallback
			config = new PluginConfig();
		}
		catch (Exception e)
		{
			String errorMsg = e.getMessage();
			if (errorMsg != null && errorMsg.length() > 100)
			{
				errorMsg = errorMsg.substring(0, 100) + "...";
			}
			plugin.getLogger().severe("CRITICAL: Failed to load config-wgefp.yml: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName()));
			plugin.getLogger().severe("File location: " + configFile.toAbsolutePath());
			plugin.getLogger().severe("Disabling plugin. Please check the file and restart the server.");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			// Use default config as fallback
			config = new PluginConfig();
		}
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

	public static boolean isJoinLocationEnabled()
	{
		return config != null ? config.getJoinLocation().isEnabled() : true; // Default to true for backward compatibility
	}
	
	public static File getConfigFile()
	{
		return configFile != null ? configFile.toFile() : null;
	}
}
