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
 * <h3>Hotbar-swap bypass fix</h3>
 * Intercepts {@code HELD_ITEM_SLOT} <em>before</em> NMS applies it. If the new slot contains a blocked
 * item the packet is cancelled outright — the player cannot equip the spear at all. This eliminates the
 * race condition without hotbar scanning or false positives.
 */
public final class DisableCompletelyProtocolLibListener extends PacketAdapter
{
	private final DisableCompletelyQuery query;

	/**
	 * Last confirmed held-slot index (0-8). Updated only when we allow a HELD_ITEM_SLOT through.
	 */
	private final Map<UUID, Integer> trackedSlot = new ConcurrentHashMap<>();

	public DisableCompletelyProtocolLibListener(Plugin plugin, DisableCompletelyQuery query)
	{
		super(plugin, ListenerPriority.LOW,
				PacketType.Play.Client.BLOCK_DIG,
				PacketType.Play.Client.HELD_ITEM_SLOT,
				PacketType.Play.Client.USE_ITEM,
				PacketType.Play.Client.USE_ENTITY,
				PacketType.Play.Client.USE_ITEM_ON,
				PacketType.Play.Client.BLOCK_PLACE,
				PacketType.Play.Client.CUSTOM_CLICK_ACTION);
		this.query = query;
	}

	/** Remove tracking data when a player leaves. */
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

		if (type == PacketType.Play.Client.USE_ITEM)
		{
			Material mat = handMaterialFromProtocolLib(event);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}
		if (type == PacketType.Play.Client.USE_ENTITY)
		{
			Material main = this.query.materialFromHand(player, true);
			Material off = this.query.materialFromHand(player, false);
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
		if (type == PacketType.Play.Client.USE_ITEM_ON || type == PacketType.Play.Client.BLOCK_PLACE)
		{
			Material mat = handMaterialFromProtocolLib(event);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}
		if (type == PacketType.Play.Client.CUSTOM_CLICK_ACTION)
		{
			Material mat = this.query.materialFromHand(player, true);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
		}
	}

	private static boolean isStabDig(EnumWrappers.PlayerDigType digType)
	{
		if (digType == null)
		{
			return false;
		}
		return "STAB".equals(digType.name());
	}

	private static Material handMaterialFromProtocolLib(PacketEvent event)
	{
		try
		{
			EnumWrappers.Hand hand = event.getPacket().getHands().read(0);
			Player p = event.getPlayer();
			if (hand == EnumWrappers.Hand.OFF_HAND)
			{
				return p.getInventory().getItemInOffHand().getType();
			}
		}
		catch (Throwable ignored)
		{
		}
		return event.getPlayer().getInventory().getItemInMainHand().getType();
	}

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
}
