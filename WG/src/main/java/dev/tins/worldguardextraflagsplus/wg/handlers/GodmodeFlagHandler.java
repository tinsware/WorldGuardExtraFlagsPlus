package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GodmodeFlagHandler extends FlagValueChangeHandler<State>
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
    public static class Factory extends Handler.Factory<GodmodeFlagHandler>
    {
		@Override
        public GodmodeFlagHandler create(Session session)
        {
            return new GodmodeFlagHandler(session);
        }
    }
    
    private static final ConcurrentHashMap<UUID, Boolean> originalWorldGuardGodmodeCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> originalEssentialsGodmodeCache = new ConcurrentHashMap<>();
    private static Object essentialsAPI;
    private static boolean essentialsChecked = false;
    
    private static void initEssentialsAPI()
    {
    	if (essentialsChecked)
    	{
    		return;
    	}
    	
    	essentialsChecked = true;
    	
    	try
    	{
    		Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
    		if (essentialsPlugin == null)
    		{
    			essentialsPlugin = Bukkit.getPluginManager().getPlugin("EssentialsX");
    		}
    		
    		if (essentialsPlugin != null)
    		{
    			Class<?> iEssentialsClass = Class.forName("net.ess3.api.IEssentials");
    			if (iEssentialsClass.isInstance(essentialsPlugin))
    			{
    				essentialsAPI = essentialsPlugin;
    			}
    		}
    	}
    	catch (ClassNotFoundException | NoClassDefFoundError e)
    	{
    		essentialsAPI = null;
    	}
    	catch (Exception e)
    	{
    		essentialsAPI = null;
    	}
    }
    
    private static boolean isEssentialsAvailable()
    {
    	initEssentialsAPI();
    	return essentialsAPI != null;
    }
	    
	protected GodmodeFlagHandler(Session session)
	{
		super(session, Flags.GODMODE);
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, State value)
	{
		this.handleValue(player, value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State currentValue, State lastValue, MoveType moveType)
	{
		this.handleValue(player, currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State lastValue, MoveType moveType)
	{
		this.handleValue(player, null);
		return true;
	}
	
	private void handleValue(LocalPlayer player, State state)
	{
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();
		if (bukkitPlayer == null || !bukkitPlayer.isOnline())
		{
			return;
		}
		
		UUID playerUUID = bukkitPlayer.getUniqueId();
		boolean hadEssentialsGodmode = false;
		boolean hadWorldGuardGodmode = false;
		
		if (state == State.DENY)
		{
			// Disable WorldGuard /god command
			try
			{
				Session session = this.getSession();
				if (session != null)
				{
					Class<?> godModeClass = Class.forName("com.sk89q.worldguard.session.handler.GodMode");
					java.lang.reflect.Method getHandlerMethod = session.getClass().getMethod("getHandler", Class.class);
					Object godModeHandler = getHandlerMethod.invoke(session, godModeClass);
					
					if (godModeHandler != null)
					{
						java.lang.reflect.Method hasGodModeMethod = godModeClass.getMethod("hasGodMode", LocalPlayer.class);
						Boolean hasGodMode = (Boolean) hasGodModeMethod.invoke(godModeHandler, player);
						
						if (hasGodMode != null && hasGodMode)
						{
							if (!originalWorldGuardGodmodeCache.containsKey(playerUUID))
							{
								originalWorldGuardGodmodeCache.put(playerUUID, true);
							}
							
							java.lang.reflect.Method setGodModeMethod = godModeClass.getMethod("set", LocalPlayer.class, Session.class, boolean.class);
							setGodModeMethod.invoke(null, player, session, false);
							hadWorldGuardGodmode = true;
						}
					}
				}
			}
			catch (Exception e)
			{
				// Silently ignore - WorldGuard godmode may not be available
			}
			
			// Disable EssentialsX godmode
			if (isEssentialsAvailable() && essentialsAPI != null)
			{
				try
				{
					Class<?> iEssentialsClass = Class.forName("net.ess3.api.IEssentials");
					java.lang.reflect.Method getUserMethod = iEssentialsClass.getMethod("getUser", Player.class);
					Object essentialsUser = getUserMethod.invoke(essentialsAPI, bukkitPlayer);
					
					if (essentialsUser != null)
					{
						Class<?> userClass = Class.forName("com.earth2me.essentials.User");
						if (userClass.isInstance(essentialsUser))
						{
							java.lang.reflect.Method isGodModeEnabledMethod = userClass.getMethod("isGodModeEnabled");
							Boolean essentialsGodmodeEnabled = (Boolean) isGodModeEnabledMethod.invoke(essentialsUser);
							
							if (essentialsGodmodeEnabled != null && essentialsGodmodeEnabled)
							{
								if (!originalEssentialsGodmodeCache.containsKey(playerUUID))
								{
									originalEssentialsGodmodeCache.put(playerUUID, true);
								}
								
								java.lang.reflect.Method setGodModeEnabledMethod = userClass.getMethod("setGodModeEnabled", boolean.class);
								setGodModeEnabledMethod.invoke(essentialsUser, false);
								hadEssentialsGodmode = true;
							}
						}
					}
				}
				catch (Exception e)
				{
					// Silently ignore - EssentialsX may not be available or player doesn't exist
				}
			}
			
			if (hadEssentialsGodmode || hadWorldGuardGodmode)
			{
				sendGodmodeDisabledMessage(bukkitPlayer);
			}
		}
		else if (state == null)
		{
			boolean autoRestore = getAutoGiveGodmodeRegionLeft();
			
			if (autoRestore)
			{
				// Restore WorldGuard godmode
				Boolean cachedWorldGuardGodmode = originalWorldGuardGodmodeCache.get(playerUUID);
				if (cachedWorldGuardGodmode != null && cachedWorldGuardGodmode)
				{
					try
					{
						Session session = this.getSession();
						if (session != null)
						{
							Class<?> godModeClass = Class.forName("com.sk89q.worldguard.session.handler.GodMode");
							java.lang.reflect.Method setGodModeMethod = godModeClass.getMethod("set", LocalPlayer.class, Session.class, boolean.class);
							setGodModeMethod.invoke(null, player, session, true);
						}
					}
					catch (Exception e)
					{
						// Silently ignore
					}
				}
				
				// Restore EssentialsX godmode
				Boolean cachedEssentialsGodmode = originalEssentialsGodmodeCache.get(playerUUID);
				if (cachedEssentialsGodmode != null && cachedEssentialsGodmode)
				{
					if (isEssentialsAvailable() && essentialsAPI != null)
					{
						try
						{
							Class<?> iEssentialsClass = Class.forName("net.ess3.api.IEssentials");
							java.lang.reflect.Method getUserMethod = iEssentialsClass.getMethod("getUser", Player.class);
							Object essentialsUser = getUserMethod.invoke(essentialsAPI, bukkitPlayer);
							
							if (essentialsUser != null)
							{
								Class<?> userClass = Class.forName("com.earth2me.essentials.User");
								if (userClass.isInstance(essentialsUser))
								{
									java.lang.reflect.Method setGodModeEnabledMethod = userClass.getMethod("setGodModeEnabled", boolean.class);
									setGodModeEnabledMethod.invoke(essentialsUser, true);
								}
							}
						}
						catch (Exception e)
						{
							// Silently ignore
						}
					}
				}
			}
			
			originalWorldGuardGodmodeCache.remove(playerUUID);
			originalEssentialsGodmodeCache.remove(playerUUID);
		}
	}
	
	private boolean getAutoGiveGodmodeRegionLeft()
	{
		try
		{
			Class<?> configClass = Class.forName("dev.tins.worldguardextraflagsplus.Config");
			java.lang.reflect.Method getMethod = configClass.getMethod("isAutoGiveGodmodeRegionLeft");
			return (Boolean) getMethod.invoke(null);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	private void sendGodmodeDisabledMessage(Player player)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		WorldGuardUtils.getScheduler().runAtEntity(player, task -> {
			Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
			if (onlinePlayer != null && onlinePlayer.isOnline())
			{
				try
				{
					Class<?> messagesClass = Class.forName("dev.tins.worldguardextraflagsplus.Messages");
					java.lang.reflect.Method sendMessageMethod = messagesClass.getMethod("sendMessageWithCooldown", 
						Player.class, String.class, String[].class);
					sendMessageMethod.invoke(null, onlinePlayer, "godmode-disabled", new String[0]);
				}
				catch (Exception e)
				{
					// Silently ignore
				}
			}
		});
	}
	
	@Override
    public State getInvincibility(LocalPlayer player)
	{
		return null;
	}
}
