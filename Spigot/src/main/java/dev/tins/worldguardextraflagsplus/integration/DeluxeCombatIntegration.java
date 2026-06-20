package dev.tins.worldguardextraflagsplus.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Soft integration with DeluxeCombat for keep-inventory combat-log handling.
 */
public final class DeluxeCombatIntegration
{
	private static final String[] IS_IN_COMBAT_CLASS_METHODS = {
			"nl.marvin.deluxecombat.api.CombatAPI|isInCombat",
			"nl.marvin.deluxecombat.api.DeluxeCombatAPI|isInCombat",
			"nl.marvin.deluxecombat.DeluxeCombat|isInCombat"
	};

	private static boolean initialized;
	private static boolean available;
	private static Object combatApiInstance;
	private static java.lang.reflect.Method isInCombatMethod;

	private DeluxeCombatIntegration()
	{
	}

	public static boolean isAvailable()
	{
		ensureInitialized();
		return available;
	}

	public static boolean isInCombat(Player player)
	{
		if (player == null)
		{
			return false;
		}

		ensureInitialized();
		if (!available || isInCombatMethod == null)
		{
			return false;
		}

		try
		{
			Object result;
			if (java.lang.reflect.Modifier.isStatic(isInCombatMethod.getModifiers()))
			{
				result = isInCombatMethod.invoke(null, player);
			}
			else
			{
				result = isInCombatMethod.invoke(combatApiInstance, player);
			}

			return result instanceof Boolean && (Boolean) result;
		}
		catch (ReflectiveOperationException ignored)
		{
			return false;
		}
	}

	private static void ensureInitialized()
	{
		if (initialized)
		{
			return;
		}

		initialized = true;

		Plugin deluxeCombat = Bukkit.getPluginManager().getPlugin("DeluxeCombat");
		if (deluxeCombat == null || !deluxeCombat.isEnabled())
		{
			return;
		}

		for (String entry : IS_IN_COMBAT_CLASS_METHODS)
		{
			String[] parts = entry.split("\\|");
			try
			{
				Class<?> apiClass = Class.forName(parts[0]);
				java.lang.reflect.Method method = apiClass.getMethod(parts[1], Player.class);
				combatApiInstance = resolveInstance(apiClass, deluxeCombat);
				isInCombatMethod = method;
				available = true;
				return;
			}
			catch (ReflectiveOperationException ignored)
			{
				// try next API shape
			}
		}
	}

	private static Object resolveInstance(Class<?> apiClass, Plugin deluxeCombat) throws ReflectiveOperationException
	{
		try
		{
			java.lang.reflect.Method getInstance = apiClass.getMethod("getInstance");
			return getInstance.invoke(null);
		}
		catch (NoSuchMethodException ignored)
		{
			if (apiClass.isInstance(deluxeCombat))
			{
				return deluxeCombat;
			}

			java.lang.reflect.Method getAPI = deluxeCombat.getClass().getMethod("getAPI");
			return getAPI.invoke(deluxeCombat);
		}
	}
}
