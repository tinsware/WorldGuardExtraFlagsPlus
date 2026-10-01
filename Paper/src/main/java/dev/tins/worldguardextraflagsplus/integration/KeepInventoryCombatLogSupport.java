package dev.tins.worldguardextraflagsplus.integration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class KeepInventoryCombatLogSnapshot
{
	private final ItemStack[] storageContents;
	private final ItemStack[] armorContents;
	private final ItemStack[] extraContents;
	private final int level;
	private final float exp;
	private final int totalExperience;

	private KeepInventoryCombatLogSnapshot(
			ItemStack[] storageContents,
			ItemStack[] armorContents,
			ItemStack[] extraContents,
			int level,
			float exp,
			int totalExperience)
	{
		this.storageContents = storageContents;
		this.armorContents = armorContents;
		this.extraContents = extraContents;
		this.level = level;
		this.exp = exp;
		this.totalExperience = totalExperience;
	}

	static KeepInventoryCombatLogSnapshot capture(Player player)
	{
		return new KeepInventoryCombatLogSnapshot(
				cloneContents(player.getInventory().getStorageContents()),
				cloneContents(player.getInventory().getArmorContents()),
				cloneContents(player.getInventory().getExtraContents()),
				player.getLevel(),
				player.getExp(),
				player.getTotalExperience());
	}

	void restore(Player player)
	{
		player.getInventory().setStorageContents(cloneContents(this.storageContents));
		player.getInventory().setArmorContents(cloneContents(this.armorContents));
		player.getInventory().setExtraContents(cloneContents(this.extraContents));
		player.setLevel(this.level);
		player.setExp(this.exp);
		player.setTotalExperience(this.totalExperience);
		player.updateInventory();
	}

	private static ItemStack[] cloneContents(ItemStack[] source)
	{
		if (source == null)
		{
			return new ItemStack[0];
		}

		ItemStack[] clone = new ItemStack[source.length];
		for (int i = 0; i < source.length; i++)
		{
			ItemStack item = source[i];
			clone[i] = item == null ? null : item.clone();
		}
		return clone;
	}
}

public final class KeepInventoryCombatLogSupport
{
	private static final Map<UUID, KeepInventoryCombatLogSnapshot> PENDING_RESTORES = new ConcurrentHashMap<>();

	private KeepInventoryCombatLogSupport()
	{
	}

	public static void rememberCombatLogQuit(Player player)
	{
		if (player == null)
		{
			return;
		}

		PENDING_RESTORES.put(player.getUniqueId(), KeepInventoryCombatLogSnapshot.capture(player));
	}

	public static void restoreIfPending(Player player)
	{
		if (player == null)
		{
			return;
		}

		KeepInventoryCombatLogSnapshot snapshot = PENDING_RESTORES.remove(player.getUniqueId());
		if (snapshot == null)
		{
			return;
		}

		snapshot.restore(player);
	}

	public static void clear(Player player)
	{
		if (player != null)
		{
			PENDING_RESTORES.remove(player.getUniqueId());
		}
	}
}
