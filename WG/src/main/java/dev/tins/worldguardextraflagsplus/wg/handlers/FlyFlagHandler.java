package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;

import lombok.Getter;
import dev.tins.worldguardextraflagsplus.flags.Flags;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyFlagHandler extends FlagValueChangeHandler<State>
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
    public static class Factory extends Handler.Factory<FlyFlagHandler>
    {
		@Override
        public FlyFlagHandler create(Session session)
        {
            return new FlyFlagHandler(session);
        }
    }

    @Getter private Boolean currentValue;
    @Setter private Boolean originalFly;

    private static final ConcurrentHashMap<UUID, Boolean> originalWorldGuardFlyCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> originalEssentialsFlyCache = new ConcurrentHashMap<>();
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

	protected FlyFlagHandler(Session session)
	{
		super(session, Flags.FLY);
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, State value)
	{
		this.handleValue(player, player.getWorld(), value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State currentValue, State lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}
	
	private void handleValue(LocalPlayer player, World world, State state)
	{
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();

		// Don't schedule tasks during shutdown
		if (!WorldGuardUtils.isPluginEnabled() || !bukkitPlayer.isOnline())
		{
			return;
		}

		UUID playerUUID = bukkitPlayer.getUniqueId();

		WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
			if (!this.getSession().getManager().hasBypass(player, world) && state != null)
			{
				boolean value = state == State.ALLOW;

				// Disable EssentialsX fly mode if it's enabled
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
								java.lang.reflect.Method getFlyMethod = userClass.getMethod("getFly");
								Boolean essentialsFlyEnabled = (Boolean) getFlyMethod.invoke(essentialsUser);

								if (essentialsFlyEnabled != null && essentialsFlyEnabled)
								{
									if (!originalEssentialsFlyCache.containsKey(playerUUID))
									{
										originalEssentialsFlyCache.put(playerUUID, true);
									}

									java.lang.reflect.Method setFlyMethod = userClass.getMethod("setFly", boolean.class);
									setFlyMethod.invoke(essentialsUser, false);
								}
							}
						}
					}
					catch (Exception e)
					{
						// Silently ignore - EssentialsX may not be available or player doesn't exist
					}
				}

				if (bukkitPlayer.getAllowFlight() != value)
				{
					if (this.originalFly == null)
					{
						this.originalFly = bukkitPlayer.getAllowFlight();
					}

					bukkitPlayer.setAllowFlight(value);
				}

				this.currentValue = value;
			}
			else
			{
				// Restore EssentialsX fly mode
				Boolean cachedEssentialsFly = originalEssentialsFlyCache.get(playerUUID);
				if (cachedEssentialsFly != null && cachedEssentialsFly)
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
									java.lang.reflect.Method setFlyMethod = userClass.getMethod("setFly", boolean.class);
									setFlyMethod.invoke(essentialsUser, true);
								}
							}
						}
						catch (Exception e)
						{
							// Silently ignore
						}
					}
				}

				if (this.originalFly != null)
				{
					bukkitPlayer.setAllowFlight(this.originalFly);

					this.originalFly = null;
				}

				this.currentValue = null;
				originalEssentialsFlyCache.remove(playerUUID);
			}
		});
	}
}



