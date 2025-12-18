package dev.tins.worldguardextraflagsplus.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.session.Session;

import lombok.RequiredArgsConstructor;
import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;
import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import dev.tins.worldguardextraflagsplus.wg.handlers.FlyFlagHandler;
import dev.tins.worldguardextraflagsplus.wg.handlers.GiveEffectsFlagHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PlayerListener implements Listener
{
	private final WorldGuardExtraFlagsPlusPlugin plugin;

	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleportEvent(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		
		player.removeMetadata(WorldGuardUtils.PREVENT_TELEPORT_LOOP_META, this.plugin);
	}

	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event)
	{
		Player player = event.getEntity();

		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
		
		Boolean keepInventory = regions.queryValue(localPlayer, Flags.KEEP_INVENTORY);
		if (keepInventory != null)
		{
			event.setKeepInventory(keepInventory);
			
			if (keepInventory)
			{
				event.getDrops().clear();
			}
		}
		
		Boolean keepExp = regions.queryValue(localPlayer, Flags.KEEP_EXP);
		if (keepExp != null)
		{
			event.setKeepLevel(keepExp);

			if (keepExp)
			{
				event.setDroppedExp(0);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event)
	{
		Player player = event.getPlayer();

		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
		
		String prefix = regions.queryValue(localPlayer, Flags.CHAT_PREFIX);
		if (prefix != null)
		{
			// Process PlaceholderAPI placeholders if available
			prefix = processPlaceholders(player, prefix);
			event.setFormat(prefix + event.getFormat());
		}

		String suffix = regions.queryValue(localPlayer, Flags.CHAT_SUFFIX);
		if (suffix != null)
		{
			// Process PlaceholderAPI placeholders if available
			suffix = processPlaceholders(player, suffix);
			event.setFormat(event.getFormat() + suffix);
		}
	}
	
	/**
	 * Processes PlaceholderAPI placeholders in a string if PlaceholderAPI is available.
	 * Falls back to returning the original string if PlaceholderAPI is not available.
	 * 
	 * @param player The player to process placeholders for
	 * @param text The text containing placeholders
	 * @return The text with placeholders processed, or original text if PAPI is not available
	 * 
	 * TODO (Optional): Add caching mechanism to avoid calling PlaceholderAPI.setPlaceholders() on every chat event.
	 * Could cache results with key: player UUID + text hash, with TTL (e.g., 1-5 seconds) to balance freshness and performance.
	 */
	private String processPlaceholders(Player player, String text)
	{
		if (text == null || text.isEmpty())
		{
			return text;
		}
		
		// Check if PlaceholderAPI is available
		if (!isPlaceholderAPIAvailable())
		{
			return text; // Return original text if PAPI is not available
		}
		
		try
		{
			// Use reflection to call PlaceholderAPI.setPlaceholders() (soft dependency)
			Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			java.lang.reflect.Method setPlaceholdersMethod = placeholderAPIClass.getMethod("setPlaceholders", Player.class, String.class);
			return (String) setPlaceholdersMethod.invoke(null, player, text);
		}
		catch (Exception e)
		{
			// PlaceholderAPI not available or error occurred - return original text
			return text;
		}
	}
	
	/**
	 * Checks if PlaceholderAPI is available on the server.
	 * 
	 * @return true if PlaceholderAPI is loaded, false otherwise
	 */
	private boolean isPlaceholderAPIAvailable()
	{
		return this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerRespawnEvent(PlayerRespawnEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		
		Location respawnLocation = this.regionContainer.createQuery().queryValue(localPlayer.getLocation(), localPlayer, Flags.RESPAWN_LOCATION);
		if (respawnLocation != null)
		{
			event.setRespawnLocation(BukkitAdapter.adapt(respawnLocation));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerItemConsumeEvent(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();
		
		ItemMeta itemMeta = event.getItem().getItemMeta();
		if (itemMeta instanceof PotionMeta potionMeta)
		{
			List<PotionEffect> effects = new ArrayList<>();
			if (potionMeta.getBasePotionType() != null)
			{
				effects.addAll(potionMeta.getBasePotionType().getPotionEffects());
			}

			effects.addAll(potionMeta.getCustomEffects());

			this.sessionManager.get(this.worldGuardPlugin.wrapPlayer(player)).getHandler(GiveEffectsFlagHandler.class).drinkPotion(player, effects);
		}
		else
		{
			Material material = event.getItem().getType();
			if (material == Material.MILK_BUCKET)
			{
				this.sessionManager.get(this.worldGuardPlugin.wrapPlayer(player)).getHandler(GiveEffectsFlagHandler.class).drinkMilk(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerGameModeChangeEvent(PlayerGameModeChangeEvent event)
	{
		Player player = event.getPlayer();
		
		Session wgSession = this.sessionManager.getIfPresent(this.worldGuardPlugin.wrapPlayer(player));
		if (wgSession != null)
		{
			Boolean value = wgSession.getHandler(FlyFlagHandler.class).getCurrentValue();
			if (value != null)
			{
				WorldGuardUtils.getScheduler().runAtEntity(player, (wrappedTask) -> checkFlyStatus(player, player.getAllowFlight()));
			}
		}
		else
		{
			WorldGuardUtils.getScheduler().runAtEntity(player, (wrappedTask) -> checkFlyStatus(player, null));
		}
	}
	
	private void checkFlyStatus(Player player, Boolean originalValueOverwrite)
	{
		FlyFlagHandler flyFlagHandler = this.sessionManager.get(this.worldGuardPlugin.wrapPlayer(player)).getHandler(FlyFlagHandler.class);

		Boolean currentValue = flyFlagHandler.getCurrentValue();
		if (currentValue != null)
		{
			player.setAllowFlight(currentValue);
		}

		if (originalValueOverwrite != null)
		{
			flyFlagHandler.setOriginalFly(originalValueOverwrite);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerItemDamageEvent(PlayerItemDamageEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);

		if (this.regionContainer.createQuery().queryState(localPlayer.getLocation(), localPlayer, Flags.ITEM_DURABILITY) == State.DENY)
		{
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerSpawnLocationEvent(PlayerSpawnLocationEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

		Location location = this.regionContainer.createQuery().queryValue(BukkitAdapter.adapt(event.getSpawnLocation()), localPlayer, Flags.JOIN_LOCATION);
		if (location != null)
		{
			event.setSpawnLocation(BukkitAdapter.adapt(location));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		Boolean flyValue = this.sessionManager.get(this.worldGuardPlugin.wrapPlayer(player)).getHandler(FlyFlagHandler.class).getCurrentValue();
		if (flyValue != null)
		{
			WorldGuardUtils.getScheduler().runAtEntity(player, (wrappedTask) -> player.setAllowFlight(flyValue));
		}
		
		// Check collision flag for players already in regions on join
		// Query the region directly to ensure collision is applied even if handler hasn't triggered yet
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
		Boolean collisionValue = regions.queryValue(localPlayer, Flags.DISABLE_COLLISION);
		if (collisionValue != null && collisionValue)
		{
			// Manually apply collision using the handler's method
			// Note: Handler uses entity scheduler internally for scoreboard operations
			WorldGuardUtils.getScheduler().runNextTick((wrappedTask) -> {
				dev.tins.worldguardextraflagsplus.wg.handlers.CollisionFlagHandler handler = 
					this.sessionManager.get(localPlayer).getHandler(dev.tins.worldguardextraflagsplus.wg.handlers.CollisionFlagHandler.class);
				if (handler != null)
				{
					handler.applyCollisionSettingPublic(player, collisionValue);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event)
	{
		Player player = event.getPlayer();

		//Some plugins toggle flight off on world change based on permissions,
		//so we need to make sure to force the flight status.
		Boolean flyValue = this.sessionManager.get(this.worldGuardPlugin.wrapPlayer(player)).getHandler(FlyFlagHandler.class).getCurrentValue();
		if (flyValue != null)
		{
			WorldGuardUtils.getScheduler().runAtEntity(player, (wrappedTask) -> player.setAllowFlight(flyValue));
		}
		
		// Check collision flag on world change
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation());
		Boolean collisionValue = regions.queryValue(localPlayer, Flags.DISABLE_COLLISION);
		if (collisionValue != null && collisionValue)
		{
			// Note: Handler uses entity scheduler internally for scoreboard operations
			WorldGuardUtils.getScheduler().runNextTick((wrappedTask) -> {
				dev.tins.worldguardextraflagsplus.wg.handlers.CollisionFlagHandler handler = 
					this.sessionManager.get(localPlayer).getHandler(dev.tins.worldguardextraflagsplus.wg.handlers.CollisionFlagHandler.class);
				if (handler != null)
				{
					handler.applyCollisionSettingPublic(player, collisionValue);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		
		if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
		{
			return;
		}
		
		Material itemType = event.getItemDrop().getItemStack().getType();
		Location location = BukkitAdapter.adapt(player.getLocation());
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(location);
		
		// Check our deny-item-drops flag (denies specific items even if WorldGuard allows drops)
		Set<Material> denySet = regions.queryValue(localPlayer, Flags.DENY_ITEM_DROPS);
		if (denySet != null && !denySet.isEmpty() && denySet.contains(itemType))
		{
			// Item is in deny list, deny it
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onEntityPickupItem(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player player))
		{
			return;
		}
		
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);
		
		if (this.sessionManager.hasBypass(localPlayer, localPlayer.getWorld()))
		{
			return;
		}
		
		Material itemType = event.getItem().getItemStack().getType();
		// Check item location for deny-item-pickup flag
		Location itemLocation = BukkitAdapter.adapt(event.getItem().getLocation());
		ApplicableRegionSet regions = this.regionContainer.createQuery().getApplicableRegions(itemLocation);
		
		// Check our deny-item-pickup flag (denies specific items even if WorldGuard allows pickups)
		Set<Material> denySet = regions.queryValue(localPlayer, Flags.DENY_ITEM_PICKUP);
		if (denySet != null && !denySet.isEmpty() && denySet.contains(itemType))
		{
			// Item is in deny list, deny it
			event.setCancelled(true);
		}
	}
	
}



