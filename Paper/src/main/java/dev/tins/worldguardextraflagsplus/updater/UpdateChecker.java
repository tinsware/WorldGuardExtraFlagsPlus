package dev.tins.worldguardextraflagsplus.updater;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Update checker for WorldGuardExtraFlagsPlus
 * Checks for updates from GitHub releases, Spigot API, and Modrinth
 * Fully compatible with Folia (uses async operations)
 */
public class UpdateChecker
{
	private final JavaPlugin plugin;
	private final String currentVersion;
	private final int spigotResourceId;
	private final String githubRepository;
	private final String modrinthProjectId;
	
	/**
	 * Creates a new UpdateChecker instance
	 * 
	 * @param plugin The plugin instance
	 * @param spigotResourceId Spigot resource ID (0 to disable Spigot checking)
	 * @param githubRepository GitHub repository in format "owner/repo" (null to disable GitHub checking)
	 * @param modrinthProjectId Modrinth project ID (null to disable Modrinth checking)
	 */
	public UpdateChecker(JavaPlugin plugin, int spigotResourceId, String githubRepository, String modrinthProjectId)
	{
		this.plugin = plugin;
		this.currentVersion = plugin.getDescription().getVersion();
		this.spigotResourceId = spigotResourceId;
		this.githubRepository = githubRepository;
		this.modrinthProjectId = modrinthProjectId;
	}
	
	/**
	 * Checks for updates asynchronously
	 * Logs results to console if update is available
	 * Checks Spigot, GitHub, and Modrinth if configured
	 * Fully Folia-compatible (uses async operations)
	 */
	public void checkForUpdates()
	{
		// Collect all update check futures
		java.util.List<CompletableFuture<UpdateResult>> futures = new java.util.ArrayList<>();
		
		// Check Spigot if resource ID is configured
		if (spigotResourceId > 0)
		{
			futures.add(checkSpigotUpdatesAsync());
		}
		
		// Check GitHub if repository is configured
		if (githubRepository != null && !githubRepository.isEmpty())
		{
			futures.add(checkGitHubUpdatesAsync());
		}
		
		// Check Modrinth if project ID is configured
		if (modrinthProjectId != null && !modrinthProjectId.isEmpty())
		{
			futures.add(checkModrinthUpdatesAsync());
		}
		
		// If none are configured, log warning
		if (futures.isEmpty())
		{
			plugin.getLogger().info("Update checker is disabled (no update sources configured)");
			return;
		}
		
		// Wait for all checks to complete, then print results
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
			java.util.List<UpdateResult> results = new java.util.ArrayList<>();
			for (CompletableFuture<UpdateResult> future : futures)
			{
				try
				{
					UpdateResult result = future.get();
					if (result != null && result.hasUpdate)
					{
						results.add(result);
					}
				}
				catch (Exception e)
				{
					// Ignore - already logged in individual checks
				}
			}
			
			// Print results if any updates found
			if (!results.isEmpty())
			{
				printUpdateResults(results);
			}
		});
	}
	
	/**
	 * Helper class to store update results
	 */
	private static class UpdateResult
	{
		final String source;
		final String latestVersion;
		final String downloadUrl;
		final boolean hasUpdate;
		
		UpdateResult(String source, String latestVersion, String downloadUrl, boolean hasUpdate)
		{
			this.source = source;
			this.latestVersion = latestVersion;
			this.downloadUrl = downloadUrl;
			this.hasUpdate = hasUpdate;
		}
	}
	
	/**
	 * Prints all update results in formatted output
	 */
	private void printUpdateResults(java.util.List<UpdateResult> results)
	{
		plugin.getLogger().info("=================== [WorldGuard ExtraFlag Plus] ======================");
		for (UpdateResult result : results)
		{
			// Remove "v" prefix from display version (show only numbers)
			String displayLatestVersion = normalizeVersion(result.latestVersion);
			plugin.getLogger().info("[" + result.source + "] An update is available! Current: " + currentVersion + " - Latest: " + displayLatestVersion);
			plugin.getLogger().info("Download: " + result.downloadUrl);
		}
		plugin.getLogger().info("=================== [WorldGuard ExtraFlag Plus] ======================");
	}
	
	/**
	 * Normalizes a version string by removing "v" prefix and trimming
	 * Also handles "v." prefix (removes both "v" and the dot)
	 * 
	 * @param version The version string (e.g., "v4.3.5", "v.4.3.5", or "4.3.5")
	 * @return Normalized version without "v" prefix (e.g., "4.3.5")
	 */
	private String normalizeVersion(String version)
	{
		if (version == null)
		{
			return null;
		}
		version = version.trim();
		// Handle "v." prefix (e.g., "v.4.3.4" -> "4.3.4")
		if (version.startsWith("v.") || version.startsWith("V."))
		{
			version = version.substring(2);
		}
		// Handle "v" prefix (e.g., "v4.3.4" -> "4.3.4")
		else if (version.startsWith("v") || version.startsWith("V"))
		{
			version = version.substring(1);
		}
		return version;
	}
	
	/**
	 * Parses a semantic version string into an integer array
	 * 
	 * @param version The version string (e.g., "4.3.5")
	 * @return Array of version parts [major, minor, patch] or null if invalid
	 */
	private int[] parseVersion(String version)
	{
		if (version == null || version.isEmpty())
		{
			return null;
		}
		
		version = normalizeVersion(version);
		
		// Remove any suffix after dash or plus (e.g., "4.3.5-beta" -> "4.3.5")
		int dashIndex = version.indexOf('-');
		int plusIndex = version.indexOf('+');
		if (dashIndex != -1 || plusIndex != -1)
		{
			int cutIndex = (dashIndex != -1 && plusIndex != -1) ? Math.min(dashIndex, plusIndex) : (dashIndex != -1 ? dashIndex : plusIndex);
			version = version.substring(0, cutIndex);
		}
		
		String[] parts = version.split("\\.");
		if (parts.length < 2)
		{
			return null; // Invalid version format
		}
		
		try
		{
			int[] versionParts = new int[parts.length];
			for (int i = 0; i < parts.length; i++)
			{
				versionParts[i] = Integer.parseInt(parts[i]);
			}
			return versionParts;
		}
		catch (NumberFormatException e)
		{
			return null; // Invalid version format
		}
	}
	
	/**
	 * Compares two semantic versions
	 * 
	 * @param version1 First version string
	 * @param version2 Second version string
	 * @return Negative if version1 < version2, positive if version1 > version2, 0 if equal, null if either is invalid
	 */
	private Integer compareVersions(String version1, String version2)
	{
		int[] v1Parts = parseVersion(version1);
		int[] v2Parts = parseVersion(version2);
		
		if (v1Parts == null || v2Parts == null)
		{
			return null; // Invalid version(s)
		}
		
		// Compare each part
		int maxLength = Math.max(v1Parts.length, v2Parts.length);
		for (int i = 0; i < maxLength; i++)
		{
			int v1Part = (i < v1Parts.length) ? v1Parts[i] : 0;
			int v2Part = (i < v2Parts.length) ? v2Parts[i] : 0;
			
			if (v1Part < v2Part)
			{
				return -1; // version1 < version2
			}
			else if (v1Part > v2Part)
			{
				return 1; // version1 > version2
			}
		}
		
		return 0; // Equal
	}
	
	/**
	 * Checks if the fetched version is newer than the current version
	 * 
	 * @param fetchedVersion The version fetched from external source
	 * @return true if fetched version is newer, false otherwise (including if version format is invalid)
	 */
	private boolean isNewerVersion(String fetchedVersion)
	{
		Integer comparison = compareVersions(fetchedVersion, currentVersion);
		if (comparison == null)
		{
			// Invalid version format - don't show update (safer than false positive)
			return false;
		}
		return comparison > 0; // fetchedVersion > currentVersion
	}
	
	/**
	 * Checks for updates from Spigot API (async version that returns a future)
	 */
	private CompletableFuture<UpdateResult> checkSpigotUpdatesAsync()
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				URL url = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + spigotResourceId).toURL();
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);
				
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
				{
					String latestVersion = reader.readLine();
					if (latestVersion != null && !latestVersion.isEmpty())
					{
						latestVersion = latestVersion.trim();
						// Add 'v' prefix for display consistency
						String displayVersion = latestVersion.startsWith("v") || latestVersion.startsWith("V") ? latestVersion : "v" + latestVersion;
						boolean hasUpdate = isNewerVersion(latestVersion);
						return new UpdateResult("Spigot", displayVersion, "https://www.spigotmc.org/resources/" + spigotResourceId, hasUpdate);
					}
				}
			}
			catch (Exception e)
			{
				plugin.getLogger().log(Level.WARNING, "Failed to check for updates from Spigot: " + e.getMessage());
			}
			return null;
		});
	}
	
	/**
	 * Checks for updates from GitHub releases (async version that returns a future)
	 */
	private CompletableFuture<UpdateResult> checkGitHubUpdatesAsync()
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				URL url = URI.create("https://api.github.com/repos/" + githubRepository + "/releases/latest").toURL();
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);
				connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
				
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
				{
					StringBuilder response = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null)
					{
						response.append(line);
					}
					
					// Simple JSON parsing for "tag_name" field
					String json = response.toString();
					int tagIndex = json.indexOf("\"tag_name\"");
					if (tagIndex != -1)
					{
						int startIndex = json.indexOf("\"", tagIndex + 10) + 1;
						int endIndex = json.indexOf("\"", startIndex);
						if (startIndex > 0 && endIndex > startIndex)
						{
							String tag = json.substring(startIndex, endIndex);
							// Keep 'v' prefix if present for display
							String displayVersion = tag;
							// Use semantic version comparison
							boolean hasUpdate = isNewerVersion(tag);
							return new UpdateResult("GitHub", displayVersion, "https://github.com/" + githubRepository + "/releases/latest", hasUpdate);
						}
					}
				}
			}
			catch (Exception e)
			{
				plugin.getLogger().log(Level.WARNING, "Failed to check for updates from GitHub: " + e.getMessage());
			}
			return null;
		});
	}
	
	/**
	 * Checks for updates from Modrinth API (async version that returns a future)
	 */
	private CompletableFuture<UpdateResult> checkModrinthUpdatesAsync()
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				URL url = URI.create("https://api.modrinth.com/v2/project/" + modrinthProjectId).toURL();
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("User-Agent", "WorldGuardExtraFlagsPlus/" + currentVersion);
				
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
				{
					StringBuilder response = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null)
					{
						response.append(line);
					}
					
					// Parse JSON for "versions" array - get the latest version
					String json = response.toString();
					
					// Find "versions" array
					int versionsIndex = json.indexOf("\"versions\"");
					if (versionsIndex != -1)
					{
						// Get versions array
						int arrayStart = json.indexOf("[", versionsIndex);
						int arrayEnd = json.indexOf("]", arrayStart);
						if (arrayStart != -1 && arrayEnd != -1)
						{
							String versionsArray = json.substring(arrayStart + 1, arrayEnd);
							// Get the first version ID (latest version is usually first)
							int firstQuote = versionsArray.indexOf("\"");
							int secondQuote = versionsArray.indexOf("\"", firstQuote + 1);
							if (firstQuote != -1 && secondQuote != -1)
							{
								String versionId = versionsArray.substring(firstQuote + 1, secondQuote);
								
								// Now fetch the version details to get the version number
								String latestVersion = fetchModrinthVersionNumber(versionId);
								if (latestVersion != null)
								{
									// Add 'v' prefix for display consistency
									String displayVersion = latestVersion.startsWith("v") || latestVersion.startsWith("V") ? latestVersion : "v" + latestVersion;
									boolean hasUpdate = isNewerVersion(latestVersion);
									return new UpdateResult("Modrinth", displayVersion, "https://modrinth.com/plugin/" + modrinthProjectId, hasUpdate);
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				plugin.getLogger().log(Level.WARNING, "Failed to check for updates from Modrinth: " + e.getMessage());
			}
			return null;
		});
	}
	
	/**
	 * Fetches the version number from Modrinth version ID
	 */
	private String fetchModrinthVersionNumber(String versionId)
	{
		try
		{
			URL url = URI.create("https://api.modrinth.com/v2/version/" + versionId).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("User-Agent", "WorldGuardExtraFlagsPlus/" + currentVersion);
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
			{
				StringBuilder response = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null)
				{
					response.append(line);
				}
				
				// Parse JSON for "version_number" field
				String json = response.toString();
				int versionNumberIndex = json.indexOf("\"version_number\"");
				if (versionNumberIndex != -1)
				{
					int startIndex = json.indexOf("\"", versionNumberIndex + 17) + 1;
					int endIndex = json.indexOf("\"", startIndex);
					if (startIndex > 0 && endIndex > startIndex)
					{
						String version = json.substring(startIndex, endIndex);
						// Remove 'v' prefix if present
						if (version.startsWith("v"))
						{
							version = version.substring(1);
						}
						return version;
					}
				}
			}
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.WARNING, "Failed to fetch Modrinth version details: " + e.getMessage());
		}
		return null;
	}
	
}

