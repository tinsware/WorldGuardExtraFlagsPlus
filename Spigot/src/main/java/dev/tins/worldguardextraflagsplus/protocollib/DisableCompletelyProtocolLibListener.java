package dev.tins.worldguardextraflagsplus.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.tins.worldguardextraflagsplus.disablecompletely.DisableCompletelyQuery;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Fallback when PacketEvents is not installed: blocks the same serverbound packets for {@code disable-completely}.
 */
public final class DisableCompletelyProtocolLibListener extends PacketAdapter
{
	private final DisableCompletelyQuery query;

	public DisableCompletelyProtocolLibListener(Plugin plugin, DisableCompletelyQuery query)
	{
		super(plugin, ListenerPriority.LOW,
				PacketType.Play.Client.BLOCK_DIG,
				PacketType.Play.Client.USE_ITEM,
				PacketType.Play.Client.USE_ENTITY,
				PacketType.Play.Client.USE_ITEM_ON,
				PacketType.Play.Client.BLOCK_PLACE,
				PacketType.Play.Client.CUSTOM_CLICK_ACTION);
		this.query = query;
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
		if (type == PacketType.Play.Client.BLOCK_DIG)
		{
			EnumWrappers.PlayerDigType digType = event.getPacket().getPlayerDigTypes().read(0);
			if (!isStabDig(digType))
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
}
