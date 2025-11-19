package dev.tins.worldguardextraflagsplus;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class Config
{
	private static JavaPlugin plugin;
	private static FileConfiguration config;
	private static File configFile;
	
	// Config values
	private static boolean permitWorkbenchBlockPlacementToo = false;
	private static boolean permitAllIncludesEnderchest = false;

	public static void initialize(JavaPlugin plugin)
	{
		Config.plugin = plugin;
		
		// Get WorldGuard plugin data folder
		File worldGuardDataFolder = plugin.getServer().getPluginManager().getPlugin("WorldGuard").getDataFolder();
		
		// Create config-wgefp.yml in WorldGuard folder
		configFile = new File(worldGuardDataFolder, "config-wgefp.yml");
		
		// Copy default config-wgefp.yml if it doesn't exist
		if (!configFile.exists())
		{
			saveDefaultConfig(worldGuardDataFolder);
		}
		
		// Load config-wgefp.yml
		reloadConfig();
	}

	private static void saveDefaultConfig(File worldGuardDataFolder)
	{
		try
		{
			// Ensure WorldGuard folder exists
			if (!worldGuardDataFolder.exists())
			{
				worldGuardDataFolder.mkdirs();
			}
			
			// Check if config-wgefp.yml already exists in WorldGuard folder
			if (configFile.exists())
			{
				// File already exists, don't overwrite it (admin might have customized it)
				plugin.getLogger().info("config-wgefp.yml already exists in WorldGuard folder, skipping default copy.");
				return;
			}
			
			// Load default config from plugin resources
			InputStream defaultStream = plugin.getResource("config-wgefp.yml");
			if (defaultStream == null)
			{
				plugin.getLogger().warning("Default config-wgefp.yml not found in plugin resources!");
				return;
			}
			
			// Copy file directly using streams
			java.io.FileOutputStream outputStream = new java.io.FileOutputStream(configFile);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = defaultStream.read(buffer)) > 0)
			{
				outputStream.write(buffer, 0, length);
			}
			outputStream.close();
			defaultStream.close();
			
			plugin.getLogger().info("Created config-wgefp.yml in WorldGuard folder: " + configFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to save default config-wgefp.yml", e);
		}
	}

	public static void reloadConfig()
	{
		try
		{
			config = YamlConfiguration.loadConfiguration(configFile);
			
			// Load UTF-8 encoding properly
			InputStream defaultStream = plugin.getResource("config-wgefp.yml");
			if (defaultStream != null)
			{
				InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8);
				YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
				config.setDefaults(defaultConfig);
			}
			
			// Load permit-workbenches settings
			permitWorkbenchBlockPlacementToo = config.getBoolean("permit-workbenches.permit-workbench-block-placement-too", false);
			permitAllIncludesEnderchest = config.getBoolean("permit-workbenches.permit-all-includes-enderchest", false);
			
			plugin.getLogger().info("Loaded config from: " + configFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to load config-wgefp.yml", e);
			// Fallback: use default values
			permitWorkbenchBlockPlacementToo = false;
			permitAllIncludesEnderchest = false;
		}
	}

	public static boolean isPermitWorkbenchBlockPlacementToo()
	{
		return permitWorkbenchBlockPlacementToo;
	}

	public static boolean isPermitAllIncludesEnderchest()
	{
		return permitAllIncludesEnderchest;
	}

	public static File getConfigFile()
	{
		return configFile;
	}
}

