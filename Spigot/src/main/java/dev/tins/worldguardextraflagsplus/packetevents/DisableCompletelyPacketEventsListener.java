package dev.tins.worldguardextraflagsplus.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import dev.tins.worldguardextraflagsplus.disablecompletely.DisableCompletelyQuery;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cancels serverbound packets that bypass Bukkit events (e.g. spear {@code Lunge} via {@link DiggingAction#STAB}).
 *
 * <p><b>Hotbar-swap bypass fix:</b>
 * A player can send {@code HELD_ITEM_CHANGE} immediately before a {@code STAB} (or other action) in the
 * same tick. By the time the action packet is processed, {@code getItemInMainHand()} already reflects the
 * new slot. We intercept {@code HELD_ITEM_CHANGE} before NMS applies it — if the target slot holds a
 * blocked item, the switch is cancelled outright. A per-player {@code trackedSlot} map then ensures
 * subsequent action packets resolve the correct material even after an allowed slot change.</p>
 */
public final class DisableCompletelyPacketEventsListener implements PacketListener
{
	private final DisableCompletelyQuery query;

	/**
	 * Last confirmed held-slot index (0-8). Updated only after we allow a HELD_ITEM_CHANGE through
	 * (i.e. the new slot is not blocked). Initialised lazily from server-side inventory.
	 */
	private final Map<UUID, Integer> trackedSlot = new ConcurrentHashMap<>();

	public DisableCompletelyPacketEventsListener(DisableCompletelyQuery query)
	{
		this.query = query;
	}

	/** Remove per-player tracking data when the player leaves. */
	public void removePlayer(UUID uuid)
	{
		trackedSlot.remove(uuid);
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		Object p = event.getPlayer();
		if (!(p instanceof Player player))
		{
			return;
		}

		PacketTypeCommon type = event.getPacketType();

		// ---- HELD_ITEM_CHANGE: intercept slot switch BEFORE NMS applies it ------
		// At this point server-side inventory still shows the old slot.
		// If the new slot contains a blocked item: cancel the switch entirely.
		if (type == PacketType.Play.Client.HELD_ITEM_CHANGE)
		{
			WrapperPlayClientHeldItemChange wrap = new WrapperPlayClientHeldItemChange(event);
			int newSlot = wrap.getSlot();
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
			// Slot is clean — record it as the new confirmed slot.
			trackedSlot.put(player.getUniqueId(), newSlot);
			return;
		}

		// ---- USE_ITEM (right-click — Riptide / charge) --------------------------
		if (type == PacketType.Play.Client.USE_ITEM)
		{
			WrapperPlayClientUseItem wrap = new WrapperPlayClientUseItem(event);
			Material mat = resolveTrackedMaterial(player, wrap.getHand());
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}

		// ---- PLAYER_DIGGING: STAB = spear jab / Lunge ---------------------------
		if (type == PacketType.Play.Client.PLAYER_DIGGING)
		{
			WrapperPlayClientPlayerDigging wrap = new WrapperPlayClientPlayerDigging(event);
			if (wrap.getAction() != DiggingAction.STAB)
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

		// ---- INTERACT_ENTITY: attack with blocked item --------------------------
		if (type == PacketType.Play.Client.INTERACT_ENTITY)
		{
			WrapperPlayClientInteractEntity wrap = new WrapperPlayClientInteractEntity(event);
			if (wrap.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK)
			{
				return;
			}
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

		// ---- PLAYER_BLOCK_PLACEMENT: right-click on block -----------------------
		if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)
		{
			WrapperPlayClientPlayerBlockPlacement wrap = new WrapperPlayClientPlayerBlockPlacement(event);
			Material mat = resolveTrackedMaterial(player, wrap.getHand());
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
			new WrapperPlayClientCustomClickAction(event);
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

	/**
	 * Returns the off-hand material. Off-hand never changes via hotbar so no
	 * tracking is needed, but a dedicated method keeps the symmetry.
	 */
	private static Material resolveTrackedOffHand(Player player)
	{
		ItemStack stack = player.getInventory().getItemInOffHand();
		return (stack != null) ? stack.getType() : Material.AIR;
	}

	/**
	 * Resolves material for the given hand, using tracked slot for main hand.
	 */
	private Material resolveTrackedMaterial(Player player, InteractionHand hand)
	{
		if (hand == InteractionHand.OFF_HAND)
		{
			return resolveTrackedOffHand(player);
		}
		return resolveTrackedMainHand(player);
	}
}
