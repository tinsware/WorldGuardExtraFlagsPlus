package dev.tins.worldguardextraflagsplus.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import dev.tins.worldguardextraflagsplus.disablecompletely.DisableCompletelyQuery;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Cancels serverbound packets that bypass Bukkit events (e.g. spear {@code Lunge} via {@link DiggingAction#STAB}).
 */
public final class DisableCompletelyPacketEventsListener implements PacketListener
{
	private final DisableCompletelyQuery query;

	public DisableCompletelyPacketEventsListener(DisableCompletelyQuery query)
	{
		this.query = query;
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
		if (type == PacketType.Play.Client.PLAYER_DIGGING)
		{
			WrapperPlayClientPlayerDigging wrap = new WrapperPlayClientPlayerDigging(event);
			DiggingAction action = wrap.getAction();
			// STAB: spear jab / Lunge (left click) — not exposed reliably via Bukkit events
			if (action != DiggingAction.STAB)
			{
				return;
			}
			Material mat = this.query.materialFromHand(player, true);
			if (this.query.isDisabled(player, mat))
			{
				event.setCancelled(true);
				this.query.sendBlocked(player, mat);
			}
			return;
		}
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

	private static Material materialForHand(Player player, InteractionHand hand)
	{
		if (hand == InteractionHand.OFF_HAND)
		{
			return player.getInventory().getItemInOffHand().getType();
		}
		return player.getInventory().getItemInMainHand().getType();
	}
}
