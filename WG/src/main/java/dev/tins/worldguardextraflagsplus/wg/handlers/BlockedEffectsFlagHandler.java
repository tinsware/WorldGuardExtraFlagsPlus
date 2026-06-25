package dev.tins.worldguardextraflagsplus.wg.handlers;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

import dev.tins.worldguardextraflagsplus.flags.Flags;
import dev.tins.worldguardextraflagsplus.flags.data.PotionEffectDetails;
import dev.tins.worldguardextraflagsplus.wg.WorldGuardUtils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BlockedEffectsFlagHandler extends FlagValueChangeHandler<Set<PotionEffectType>> {
	public static final Factory FACTORY() {
		return new Factory();
	}
	
    public static class Factory extends Handler.Factory<BlockedEffectsFlagHandler>
    {
		@Override
        public BlockedEffectsFlagHandler create(Session session)
        {
            return new BlockedEffectsFlagHandler(session);
        }
    }
	
	private ConcurrentHashMap<PotionEffectType, PotionEffectDetails> removedEffects;
    
	protected BlockedEffectsFlagHandler(Session session)
	{
		super(session, Flags.BLOCKED_EFFECTS);
		
		this.removedEffects = new ConcurrentHashMap<>();
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Set<PotionEffectType> value)
	{
		this.handleValue(player, player.getWorld(), value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<PotionEffectType> currentValue, Set<PotionEffectType> lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<PotionEffectType> lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}

	@Override
	public void tick(LocalPlayer player, ApplicableRegionSet set)
	{
		this.handleValue(player, player.getWorld(), set.queryValue(player, Flags.BLOCKED_EFFECTS));
	}
	
	private void handleValue(LocalPlayer player, World world, Set<PotionEffectType> value)
	{
		if (value == null && this.removedEffects.isEmpty())
		{
			return;
		}

		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();

		if (!WorldGuardUtils.isPluginEnabled() || !bukkitPlayer.isOnline())
		{
			return;
		}

		boolean shouldBlock = value != null && !this.getSession().getManager().hasBypass(player, world);

		WorldGuardUtils.getScheduler().runAtEntity(bukkitPlayer, task -> {
			if (shouldBlock)
			{
				for (PotionEffectType effectType : value)
				{
					PotionEffect effect = null;
					for (PotionEffect activeEffect : bukkitPlayer.getActivePotionEffects())
					{
						if (activeEffect.getType().equals(effectType))
						{
							effect = activeEffect;
							break;
						}
					}

					if (effect != null)
					{
						this.removedEffects.put(effect.getType(), new PotionEffectDetails(System.nanoTime() + (long) (effect.getDuration() / 20D * TimeUnit.SECONDS.toNanos(1L)), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
						bukkitPlayer.removePotionEffect(effectType);
					}
				}
			}

			Iterator<Entry<PotionEffectType, PotionEffectDetails>> iterator = this.removedEffects.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<PotionEffectType, PotionEffectDetails> entry = iterator.next();
				PotionEffectType type = entry.getKey();

				if (value == null || !value.contains(type))
				{
					PotionEffectDetails details = entry.getValue();
					int timeLeft = details.getTimeLeftInTicks();

					if (timeLeft > 0)
					{
						bukkitPlayer.addPotionEffect(new PotionEffect(type, timeLeft, details.getAmplifier(), details.isAmbient(), details.isParticles()), true);
					}

					iterator.remove();
				}
			}
		});
	}
}



