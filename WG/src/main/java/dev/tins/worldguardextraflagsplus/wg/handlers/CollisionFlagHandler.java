package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import lombok.Getter;
import org.bukkit.entity.Player;

public class CollisionFlagHandler extends FlagValueChangeHandler<Boolean>
{
	@Getter
	private Boolean currentValue;
	
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
	public static class Factory extends Handler.Factory<CollisionFlagHandler>
	{
		@Override
		public CollisionFlagHandler create(Session session)
		{
			return new CollisionFlagHandler(session);
		}
	}
	
	protected CollisionFlagHandler(Session session)
	{
		super(session, Flags.DISABLE_COLLISION);
	}
	
	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Boolean value)
	{
		this.handleValue(player, player.getWorld(), value);
	}
	
	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Boolean currentValue, Boolean lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}
	
	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Boolean lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}
	
	private void handleValue(LocalPlayer player, World world, Boolean disableCollision)
	{
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();
		
		// Don't schedule tasks during shutdown
		if (!WorldGuardUtils.isPluginEnabled() || bukkitPlayer == null || !bukkitPlayer.isOnline())
		{
			return;
		}
		
		// Check if player has bypass
		if (this.getSession().getManager().hasBypass(player, world))
		{
			// Restore collision if has bypass
			WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
				Player onlinePlayer = org.bukkit.Bukkit.getPlayer(bukkitPlayer.getUniqueId());
				if (onlinePlayer != null && onlinePlayer.isOnline())
				{
					applyCollisionSetting(onlinePlayer, false);
				}
			});
			return;
		}
		
		// Store current value
		this.currentValue = disableCollision;
		
		// Capture player UUID before scheduling (important for Folia thread safety)
		java.util.UUID playerUUID = bukkitPlayer.getUniqueId();
		
		// Apply collision setting using packet handler
		WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
			Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUUID);
			if (onlinePlayer == null || !onlinePlayer.isOnline())
			{
				return;
			}
			
			applyCollisionSetting(onlinePlayer, disableCollision);
		});
	}
	
	/**
	 * Apply collision setting using packet handler.
	 * If disableCollision is true, collision is disabled. If false or null, collision is enabled.
	 * Uses reflection to access Spigot module classes (WG module can't directly depend on Spigot module).
	 */
	private void applyCollisionSetting(Player player, Boolean disableCollision)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		try
		{
			// Use reflection to access plugin class from Spigot module
			Class<?> pluginClass = Class.forName("dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin");
			java.lang.reflect.Method getPluginMethod = pluginClass.getMethod("getPlugin");
			Object plugin = getPluginMethod.invoke(null);
			
			if (plugin == null)
			{
				org.bukkit.Bukkit.getLogger().warning("[Collision] Plugin instance is null!");
				return;
			}
			
			// Get packet handler from plugin using reflection
			java.lang.reflect.Method getPacketHandlerMethod = pluginClass.getMethod("getCollisionPacketHandler");
			Object packetHandler = getPacketHandlerMethod.invoke(plugin);
			
			if (packetHandler == null)
			{
				org.bukkit.Bukkit.getLogger().warning("[Collision] Packet handler is null - collision feature is disabled");
				return;
			}
			
			// Call appropriate method on packet handler
			Class<?> handlerClass = packetHandler.getClass();
			if (disableCollision != null && disableCollision)
			{
				// Disable collision
				java.lang.reflect.Method disableMethod = handlerClass.getMethod("disableCollision", Player.class);
				disableMethod.invoke(packetHandler, player);
			}
			else
			{
				// Enable collision (restore normal collision)
				java.lang.reflect.Method enableMethod = handlerClass.getMethod("enableCollision", Player.class);
				enableMethod.invoke(packetHandler, player);
			}
		}
		catch (ClassNotFoundException e)
		{
			org.bukkit.Bukkit.getLogger().warning("[Collision] Plugin class not found - Spigot module not loaded: " + e.getMessage());
		}
		catch (NoSuchMethodException e)
		{
			org.bukkit.Bukkit.getLogger().warning("[Collision] Method not found - API mismatch: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
		}
		catch (Exception e)
		{
			org.bukkit.Bukkit.getLogger().warning("[Collision] Failed to apply collision setting: " + 
				(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			e.printStackTrace();
		}
	}
	
	/**
	 * Public method to manually apply collision settings.
	 * Useful for ensuring collision is applied on player join.
	 */
	public void applyCollisionSettingPublic(Player player, Boolean disableCollision)
	{
		if (player == null || !player.isOnline())
		{
			return;
		}
		
		applyCollisionSetting(player, disableCollision);
	}
}
