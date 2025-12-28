package dev.tins.worldguardextraflagsplus.protocollib;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.sk89q.worldguard.session.Session;

import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;
import dev.tins.worldguardextraflagsplus.wg.handlers.GiveEffectsFlagHandler;

public class RemoveEffectPacketListener extends PacketAdapter
{
	public RemoveEffectPacketListener()
	{
		super(WorldGuardExtraFlagsPlusPlugin.getPlugin(WorldGuardExtraFlagsPlusPlugin.class), PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
	}
	
	@Override
	public void onPacketSending(PacketEvent event)
	{
		if (!event.isCancelled())
		{
			Player player = event.getPlayer();
			if (!player.isValid()) //Work around, getIfPresent is broken inside WG due to using LocalPlayer as key instead of CacheKey
			{
				return;
			}

			try
			{
				Session session = WorldGuard.getInstance().getPlatform().getSessionManager().get(WorldGuardPlugin.inst().wrapPlayer(player));
				
				GiveEffectsFlagHandler giveEffectsHandler = session.getHandler(GiveEffectsFlagHandler.class);
				if (giveEffectsHandler.isSupressRemovePotionPacket())
				{
					event.setCancelled(true);
				}
			}
			catch(IllegalStateException wgBug)
			{
				
			}
		}
	}
}



