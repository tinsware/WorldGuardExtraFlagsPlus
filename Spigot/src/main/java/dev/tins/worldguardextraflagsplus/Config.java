package dev.tins.worldguardextraflagsplus;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;

public class Config
{
	private static JavaPlugin plugin;
	private static FileConfiguration config;
	private static File configFile;
	
	// Config values
	private static boolean permitWorkbenchBlockPlacementToo = false;
	private static boolean permitAllIncludesEnderchest = false;
	private static boolean autoGiveGodmodeRegionLeft = false;

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
			// Load file without defaults first to check actual file content
			config = YamlConfiguration.loadConfiguration(configFile);
			
			// Load UTF-8 encoding properly
			InputStream defaultStream = plugin.getResource("config-wgefp.yml");
			YamlConfiguration defaultConfig = null;
			if (defaultStream != null)
			{
				InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8);
				defaultConfig = YamlConfiguration.loadConfiguration(reader);
				config.setDefaults(defaultConfig);
			}
			
			// Check if godmode.auto-give-godmode-region-left key exists in actual file
			boolean needsSave = false;
			String fileContent = null;
			if (configFile.exists())
			{
				try
				{
					fileContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
					String originalContent = fileContent;
					
					// Check if godmode section exists
					if (!fileContent.contains("godmode:") || !fileContent.contains("auto-give-godmode-region-left:"))
					{
						// Add godmode section if missing
						if (!fileContent.contains("godmode:"))
						{
							// Add after permit-workbenches section
							if (fileContent.contains("permit-all-includes-enderchest:"))
							{
								int insertIndex = fileContent.lastIndexOf("permit-all-includes-enderchest:");
								int lineEnd = fileContent.indexOf('\n', insertIndex);
								if (lineEnd == -1)
								{
									lineEnd = fileContent.length();
								}
								else
								{
									lineEnd++; // Include the newline
								}
								
								String newSection = "\n\n# Godmode settings\ngodmode:\n  # If true, automatically restore godmode when player leaves a region\n  # Default: false\n  auto-give-godmode-region-left: false";
								fileContent = fileContent.substring(0, lineEnd) + newSection + fileContent.substring(lineEnd);
							}
							else
							{
								// Fallback: append at the end
								fileContent = fileContent + "\n\n# Godmode settings\ngodmode:\n  # If true, automatically restore godmode when player leaves a region\n  # Default: false\n  auto-give-godmode-region-left: false";
							}
						}
						else if (!fileContent.contains("auto-give-godmode-region-left:"))
						{
							// godmode section exists but missing the key
							int godmodeIndex = fileContent.indexOf("godmode:");
							int lineEnd = fileContent.indexOf('\n', godmodeIndex);
							if (lineEnd == -1)
							{
								lineEnd = fileContent.length();
							}
							else
							{
								lineEnd++; // Include the newline
							}
							
							// Find the end of the godmode section (next top-level key or end of file)
							int nextSectionStart = fileContent.length();
							for (int i = lineEnd; i < fileContent.length(); i++)
							{
								char c = fileContent.charAt(i);
								if (c == '\n' || c == '\r')
								{
									// Check if this line starts a new top-level key (no leading spaces)
									int lineStart = i + 1;
									while (lineStart < fileContent.length() && (fileContent.charAt(lineStart) == ' ' || fileContent.charAt(lineStart) == '\t'))
									{
										lineStart++;
									}
									if (lineStart < fileContent.length() && fileContent.charAt(lineStart) != '#' && fileContent.charAt(lineStart) != '\n' && fileContent.charAt(lineStart) != '\r')
									{
										// Check if it's a top-level key (no leading spaces/tabs)
										int checkPos = i + 1;
										while (checkPos < fileContent.length() && (fileContent.charAt(checkPos) == ' ' || fileContent.charAt(checkPos) == '\t'))
										{
											checkPos++;
										}
										if (checkPos < fileContent.length() && fileContent.charAt(checkPos) != ' ' && fileContent.charAt(checkPos) != '\t')
										{
											nextSectionStart = i + 1;
											break;
										}
									}
								}
							}
							
							String newKey = "  # If true, automatically restore godmode when player leaves a region\n  # Default: false\n  auto-give-godmode-region-left: false";
							fileContent = fileContent.substring(0, nextSectionStart) + newKey + "\n" + fileContent.substring(nextSectionStart);
						}
						
						if (!fileContent.equals(originalContent))
						{
							needsSave = true;
							plugin.getLogger().info("Added missing 'godmode.auto-give-godmode-region-left' key to config-wgefp.yml");
						}
					}
				}
				catch (Exception e)
				{
					plugin.getLogger().log(Level.WARNING, "Error checking config-wgefp.yml for missing keys: " + e.getMessage(), e);
				}
			}
			
			// Save if changes were made
			if (needsSave && fileContent != null)
			{
				Files.write(configFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
				plugin.getLogger().info("Updated config-wgefp.yml with new keys");
			}
			
			// Now reload with defaults for actual use
			config = YamlConfiguration.loadConfiguration(configFile);
			if (defaultConfig != null)
			{
				config.setDefaults(defaultConfig);
			}
			
			// Load permit-workbenches settings
			permitWorkbenchBlockPlacementToo = config.getBoolean("permit-workbenches.permit-workbench-block-placement-too", false);
			permitAllIncludesEnderchest = config.getBoolean("permit-workbenches.permit-all-includes-enderchest", false);
			
			// Load godmode settings
			autoGiveGodmodeRegionLeft = config.getBoolean("godmode.auto-give-godmode-region-left", false);
			
			plugin.getLogger().info("Loaded config from: " + configFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Failed to load config-wgefp.yml", e);
			// Fallback: use default values
			permitWorkbenchBlockPlacementToo = false;
			permitAllIncludesEnderchest = false;
			autoGiveGodmodeRegionLeft = false;
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

	public static boolean isAutoGiveGodmodeRegionLeft()
	{
		return autoGiveGodmodeRegionLeft;
	}

	public static File getConfigFile()
	{
		return configFile;
	}
}

