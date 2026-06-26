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
 * <h3>Hotbar-swap bypass</h3>
 * The client can send {@code HELD_ITEM_CHANGE} (slot switch) immediately before a {@code STAB} packet.
 * Both arrive in the same server tick. By the time NMS processes {@code STAB} it already reads the new
 * slot — so does our listener if we call {@link Player#getInventory()#getItemInMainHand()}.
 *
 * <p>Fix: we intercept {@code HELD_ITEM_CHANGE} <em>before</em> it is applied by NMS. At that moment the
 * server-side inventory still reflects the <em>old</em> slot. We record what material is in the
 * <em>new</em> (requested) slot. If it is a blocked item we cancel {@code HELD_ITEM_CHANGE} outright —
 * the player cannot even equip the blocked spear. This is the cleanest possible solution: no hotbar scan,
 * no race condition, no false positives.</p>
 *
 * <p>Additionally we still track the last confirmed slot so that a {@code STAB} for an already-equipped
 * blocked item (no slot change needed) is also caught via {@link #resolveTrackedMainHand}.</p>
 */
public final class DisableCompletelyPacketEventsListener implements PacketListener
{
	private final DisableCompletelyQuery query;

	/**
	 * Last <em>confirmed</em> held-slot index (0-8) — updated only after we allow a HELD_ITEM_CHANGE
	 * through (i.e. the new slot is not blocked). Initialized lazily from server-side inventory.
	 */
	private final Map<UUID, Integer> trackedSlot = new ConcurrentHashMap<>();

	public DisableCompletelyPacketEventsListener(DisableCompletelyQuery query)
	{
		this.query = query;
	}

	/** Remove tracking data when a player leaves to avoid memory leaks. */
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
		// This is the primary defence against the hotbar-swap bypass.
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
				// Block the slot switch — player cannot equip the blocked item.
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
			Material mat = materialForHand(player, wrap.getHand());
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}

		// ---- PLAYER_DIGGING: STAB = spear jab / Lunge ---------------------------
		// By the time we see STAB, HELD_ITEM_CHANGE was either blocked (so the old slot
		// is still active) or allowed (trackedSlot updated). resolveTrackedMainHand gives
		// the correct picture in both cases.
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

		// ---- PLAYER_BLOCK_PLACEMENT: right-click on block -----------------------
		if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)
		{
			WrapperPlayClientPlayerBlockPlacement wrap = new WrapperPlayClientPlayerBlockPlacement(event);
			Material mat = materialForHand(player, wrap.getHand());
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
			Material mat = this.query.materialFromHand(player, true);
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

	private static Material materialForHand(Player player, InteractionHand hand)
	{
		if (hand == InteractionHand.OFF_HAND)
		{
			return player.getInventory().getItemInOffHand().getType();
		}
		return player.getInventory().getItemInMainHand().getType();
	}

	/**
	 * Returns the material in the slot last confirmed by our HELD_ITEM_CHANGE handler.
	 * Falls back to server-side main hand if no tracking exists yet (player joined with
	 * a blocked item already selected, or first action before any slot change).
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
}
