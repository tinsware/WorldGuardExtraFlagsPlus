package dev.tins.worldguardextraflagsplus.wg.handlers;

import java.util.*;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;

import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;

import dev.tins.worldguardextraflagsplus.flags.Flags;

public class CommandOnExitFlagHandler extends Handler
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
    public static class Factory extends Handler.Factory<CommandOnExitFlagHandler>
    {
		@Override
        public CommandOnExitFlagHandler create(Session session)
        {
            return new CommandOnExitFlagHandler(session);
        }
    }
	
	private Collection<Set<String>> lastCommands;
	    
	protected CommandOnExitFlagHandler(Session session)
	{
		super(session);
		
		this.lastCommands = new ArrayList<>();
	}
	
    @Override
	public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set)
    {
    	this.lastCommands = set.queryAllValues(player, Flags.COMMAND_ON_EXIT);
    }
    	
	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		Collection<Set<String>> commands = Lists.newArrayList(toSet.queryAllValues(player, Flags.COMMAND_ON_EXIT));
		
		if (!commands.isEmpty())
		{
			for (ProtectedRegion region : toSet)
			{
                Set<String> commands_ = region.getFlag(Flags.COMMAND_ON_EXIT);
                if (commands_ != null)
                {
                	commands.add(commands_);
                }
            }
		}

		Collection<Set<String>> lastCommands = this.lastCommands;

		this.lastCommands = commands;

		if (!this.getSession().getManager().hasBypass(player, (World) to.getExtent()))
		{
			org.bukkit.entity.Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();
			for (Set<String> commands_ : lastCommands)
			{
				if (!commands.contains(commands_) && commands_.size() > 0)
				{
					for (String command : commands_) {
						WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, (wrappedTask) -> Bukkit.getServer().dispatchCommand(bukkitPlayer, command.substring(1).replace("%username%", player.getName()))); //TODO: Make this better
					}

					break;
				}
			}
		}
		
		return true;
	}
}



