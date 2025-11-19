package dev.tins.worldguardextraflagsplus.commands;

import dev.tins.worldguardextraflagsplus.Messages;
import dev.tins.worldguardextraflagsplus.Config;
import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter
{
	private final WorldGuardExtraFlagsPlusPlugin plugin;

	public ReloadCommand(WorldGuardExtraFlagsPlusPlugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (args.length == 0)
		{
			sender.sendMessage("§eUsage: /" + label + " reload");
			return true;
		}

		if (!args[0].equalsIgnoreCase("reload"))
		{
			sender.sendMessage("§cUnknown subcommand: " + args[0]);
			sender.sendMessage("§eUsage: /" + label + " reload");
			return true;
		}

		if (!sender.hasPermission("worldguardextraflagsplus.reload"))
		{
			sender.sendMessage("§cYou don't have permission to use this command.");
			return true;
		}

		try
		{
			// Reload messages and config
			Messages.reloadMessages();
			Config.reloadConfig();
			
			sender.sendMessage("§aMessages and config reloaded successfully!");
			plugin.getLogger().info("Messages and config reloaded by " + sender.getName());
		}
		catch (Exception e)
		{
			sender.sendMessage("§cFailed to reload messages/config: " + e.getMessage());
			plugin.getLogger().severe("Failed to reload messages/config: " + e.getMessage());
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
	{
		if (args.length == 1)
		{
			// Tab complete for "reload"
			List<String> completions = new ArrayList<>();
			if ("reload".startsWith(args[0].toLowerCase()))
			{
				completions.add("reload");
			}
			return completions;
		}
		
		return Collections.emptyList();
	}
}

