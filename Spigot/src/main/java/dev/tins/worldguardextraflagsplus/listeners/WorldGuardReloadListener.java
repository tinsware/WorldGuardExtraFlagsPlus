package dev.tins.worldguardextraflagsplus.listeners;

import dev.tins.worldguardextraflagsplus.Messages;
import dev.tins.worldguardextraflagsplus.Config;
import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class WorldGuardReloadListener implements Listener
{
	private final WorldGuardExtraFlagsPlusPlugin plugin;

	public WorldGuardReloadListener(WorldGuardExtraFlagsPlusPlugin plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		String command = event.getMessage().toLowerCase().trim();
		
		// Check if command is /wg reload or /worldguard reload (handles multiple spaces)
		if (command.matches("^/wg\\s+reload.*") || command.matches("^/worldguard\\s+reload.*"))
		{
			try
			{
				// Reload our messages and config when WorldGuard reload is triggered
				Messages.reloadMessages();
				Config.reloadConfig();
				plugin.getLogger().info("Messages and config reloaded automatically due to WorldGuard reload command");
			}
			catch (Exception e)
			{
				plugin.getLogger().warning("Failed to reload messages/config during WorldGuard reload: " + e.getMessage());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onConsoleCommand(ServerCommandEvent event)
	{
		String command = event.getCommand().toLowerCase().trim();
		
		// Check if command is wg reload or worldguard reload (handles multiple spaces)
		if (command.matches("^wg\\s+reload.*") || command.matches("^worldguard\\s+reload.*"))
		{
			try
			{
				// Reload our messages and config when WorldGuard reload is triggered
				Messages.reloadMessages();
				Config.reloadConfig();
				plugin.getLogger().info("Messages and config reloaded automatically due to WorldGuard reload command");
			}
			catch (Exception e)
			{
				plugin.getLogger().warning("Failed to reload messages/config during WorldGuard reload: " + e.getMessage());
			}
		}
	}
}

