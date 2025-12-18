package dev.tins.worldguardextraflagsplus.collision;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Integration with TAB plugin for collision management.
 * Uses TAB's API to set collision rules, which prevents TAB from overwriting our changes.
 */
public class TabIntegration
{
	private static final String TAB_PLUGIN_NAME = "TAB";
	private static Object tabApiInstance = null;
	private static Object nameTagManager = null;
	private static Method getPlayerMethod = null;
	private static Method setCollisionRuleMethod = null;
	private static boolean initialized = false;
	
	/**
	 * Initializes TAB integration.
	 * @return true if TAB is available and integration was successful, false otherwise
	 */
	public static boolean initialize()
	{
		if (initialized)
		{
			return nameTagManager != null;
		}
		
		initialized = true;
		
		try
		{
			Plugin tabPlugin = Bukkit.getPluginManager().getPlugin(TAB_PLUGIN_NAME);
			if (tabPlugin == null)
			{
				return false;
			}
			
			// Get TAB API instance using reflection
			Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
			Method getInstanceMethod = tabApiClass.getMethod("getInstance");
			tabApiInstance = getInstanceMethod.invoke(null);
			
			if (tabApiInstance == null)
			{
				return false;
			}
			
			// Get NameTagManager
			Method getNameTagManagerMethod = tabApiClass.getMethod("getNameTagManager");
			nameTagManager = getNameTagManagerMethod.invoke(tabApiInstance);
			
			if (nameTagManager == null)
			{
				return false;
			}
			
			// Get methods we need
			Class<?> nameTagManagerClass = nameTagManager.getClass().getInterfaces()[0];
			setCollisionRuleMethod = nameTagManagerClass.getMethod("setCollisionRule", 
				Class.forName("me.neznamy.tab.api.TabPlayer"), Boolean.class);
			
			// Get player method from TabAPI
			getPlayerMethod = tabApiClass.getMethod("getPlayer", java.util.UUID.class);
			
			return true;
		}
		catch (Exception e)
		{
			// TAB not available or API changed
			tabApiInstance = null;
			nameTagManager = null;
			return false;
		}
	}
	
	/**
	 * Checks if TAB integration is available.
	 * @return true if TAB is available and initialized, false otherwise
	 */
	public static boolean isAvailable()
	{
		return initialized && nameTagManager != null;
	}
	
	/**
	 * Sets collision rule for a player using TAB's API.
	 * @param player The player to set collision for
	 * @param collision true to disable collision, false to enable collision, null to remove forced value
	 * @return true if successful, false if TAB is not available or operation failed
	 */
	public static boolean setCollisionRule(Player player, Boolean collision)
	{
		if (!isAvailable() || player == null)
		{
			return false;
		}
		
		try
		{
			// Get TabPlayer from TabAPI
			Object tabPlayer = getPlayerMethod.invoke(tabApiInstance, player.getUniqueId());
			if (tabPlayer == null)
			{
				return false;
			}
			
			// Set collision rule using TAB's API
			// TAB uses: true = collision enabled, false = collision disabled
			// We use: true = disable collision, false = enable collision
			// So we need to invert: if we want to disable collision, set TAB to false
			Boolean tabCollision = collision == null ? null : !collision;
			setCollisionRuleMethod.invoke(nameTagManager, tabPlayer, tabCollision);
			
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}

