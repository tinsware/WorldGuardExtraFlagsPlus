package dev.tins.worldguardextraflagsplus.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.tins.worldguardextraflagsplus.disablecompletely.DisableCompletelyQuery;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback when PacketEvents is not installed: blocks the same serverbound packets for {@code disable-completely}.
 *
 * <p><b>Hotbar-swap bypass fix:</b>
 * Intercepts {@code HELD_ITEM_SLOT} before NMS applies it. If the new slot contains a blocked item
 * the packet is cancelled outright — the player cannot equip the spear. A per-player
 * {@code trackedSlot} map ensures subsequent action packets resolve the correct material.</p>
 */
public final class DisableCompletelyProtocolLibListener extends PacketAdapter
{
	private final DisableCompletelyQuery query;

	/**
	 * Last confirmed held-slot index (0-8). Updated only after we allow a HELD_ITEM_SLOT through.
	 */
	private final Map<UUID, Integer> trackedSlot = new ConcurrentHashMap<>();

	public DisableCompletelyProtocolLibListener(Plugin plugin, DisableCompletelyQuery query)
	{
		super(plugin, ListenerPriority.LOW,
				PacketType.Play.Client.HELD_ITEM_SLOT,
				PacketType.Play.Client.BLOCK_DIG,
				PacketType.Play.Client.USE_ITEM,
				PacketType.Play.Client.USE_ENTITY,
				PacketType.Play.Client.USE_ITEM_ON,
				PacketType.Play.Client.BLOCK_PLACE,
				PacketType.Play.Client.CUSTOM_CLICK_ACTION);
		this.query = query;
	}

	/** Remove per-player tracking data when the player leaves. */
	public void removePlayer(UUID uuid)
	{
		trackedSlot.remove(uuid);
	}

	@Override
	public void onPacketReceiving(PacketEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		if (!(event.getPlayer() instanceof Player player))
		{
			return;
		}
		PacketType type = event.getPacketType();

		// ---- HELD_ITEM_SLOT: intercept BEFORE NMS applies the slot change -------
		if (type == PacketType.Play.Client.HELD_ITEM_SLOT)
		{
			try
			{
				int newSlot = event.getPacket().getIntegers().read(0);
				if (newSlot < 0 || newSlot > 8)
				{
					return;
				}
				PlayerInventory inv = player.getInventory();
				ItemStack stack = inv.getItem(newSlot);
				Material mat = (stack != null) ? stack.getType() : Material.AIR;

				if (this.query.isDisabled(player, mat))
				{
					event.setCancelled(true);
					this.query.sendBlocked(player, mat);
					return;
				}
				trackedSlot.put(player.getUniqueId(), newSlot);
			}
			catch (Throwable ignored)
			{
			}
			return;
		}

		// ---- BLOCK_DIG: STAB = spear jab / Lunge --------------------------------
		if (type == PacketType.Play.Client.BLOCK_DIG)
		{
			EnumWrappers.PlayerDigType digType = event.getPacket().getPlayerDigTypes().read(0);
			if (!isStabDig(digType))
			{
				return;
			}
			Material mat = resolveTrackedMainHand(player);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}

		// ---- USE_ITEM (right-click — Riptide / charge) --------------------------
		if (type == PacketType.Play.Client.USE_ITEM)
		{
			Material mat = resolveTrackedHandFromProtocolLib(event);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}

		// ---- USE_ENTITY: attack entity with blocked item ------------------------
		if (type == PacketType.Play.Client.USE_ENTITY)
		{
			Material main = resolveTrackedMainHand(player);
			Material off = resolveTrackedOffHand(player);
			if (this.query.isDisabled(player, main))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, main);
			}
			else if (this.query.isDisabled(player, off))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, off);
			}
			return;
		}

		// ---- USE_ITEM_ON / BLOCK_PLACE: right-click on block --------------------
		if (type == PacketType.Play.Client.USE_ITEM_ON || type == PacketType.Play.Client.BLOCK_PLACE)
		{
			Material mat = resolveTrackedHandFromProtocolLib(event);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}

		// ---- CUSTOM_CLICK_ACTION (1.21.11+ spear quick-attack) ------------------
		if (type == PacketType.Play.Client.CUSTOM_CLICK_ACTION)
		{
			Material mat = resolveTrackedMainHand(player);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static boolean isStabDig(EnumWrappers.PlayerDigType digType)
	{
		if (digType == null)
		{
			return false;
		}
		return "STAB".equals(digType.name());
	}

	/**
	 * Returns the material in the last confirmed held slot, falling back to
	 * server-side main hand if no tracking exists yet.
	 */
	private Material resolveTrackedMainHand(Player player)
	{
		Integer slot = trackedSlot.get(player.getUniqueId());
		if (slot == null)
		{
			return this.query.materialFromHand(player, true);
		}
		PlayerInventory inv = player.getInventory();
		ItemStack stack = inv.getItem(slot);
		return (stack != null) ? stack.getType() : Material.AIR;
	}

	private static Material resolveTrackedOffHand(Player player)
	{
		ItemStack stack = player.getInventory().getItemInOffHand();
		return (stack != null) ? stack.getType() : Material.AIR;
	}

	/**
	 * Reads the hand from a ProtocolLib packet (USE_ITEM / USE_ITEM_ON / BLOCK_PLACE)
	 * and resolves the tracked material, so main-hand is read from {@code trackedSlot}
	 * rather than {@code getItemInMainHand()}.
	 */
	private Material resolveTrackedHandFromProtocolLib(PacketEvent event)
	{
		try
		{
			EnumWrappers.Hand hand = event.getPacket().getHands().read(0);
			if (hand == EnumWrappers.Hand.OFF_HAND)
			{
				return resolveTrackedOffHand(event.getPlayer());
			}
		}
		catch (Throwable ignored)
		{
		}
		return resolveTrackedMainHand(event.getPlayer());
	}
}
