package dev.tins.worldguardextraflagsplus.flags.helpers;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

import java.util.HashSet;
import java.util.Set;

public class BlockableItemFlag extends Flag<String>
{
	// Hardcoded list of items that can be blocked by disable-completely flag (or deprecated permit-completely flag)
	// TODO: Add more items in future updates
	private static final Set<String> BLOCKABLE_ITEMS = new HashSet<>();
	static
	{
		BLOCKABLE_ITEMS.add("MACE");
		BLOCKABLE_ITEMS.add("FIREWORK_ROCKET");
		BLOCKABLE_ITEMS.add("WIND_CHARGE");
		BLOCKABLE_ITEMS.add("TOTEM_OF_UNDYING");
		BLOCKABLE_ITEMS.add("TRIDENT");
	}

	public BlockableItemFlag(String name)
	{
		super(name);
	}

	@Override
	public Object marshal(String o)
	{
		return o;
	}

	@Override
	public String parseInput(FlagContext context) throws InvalidFlagFormat
	{
		String input = context.getUserInput().trim();
		
		// Handle empty input
		if (input.isEmpty())
		{
			throw new InvalidFlagFormat("Item name cannot be empty");
		}
		
		// Convert to uppercase for comparison
		String upperInput = input.toUpperCase();
		
		// Validate against hardcoded list
		if (!BLOCKABLE_ITEMS.contains(upperInput))
		{
			throw new InvalidFlagFormat("Invalid item '" + input + "'. Only the following items can be blocked: " + String.join(", ", BLOCKABLE_ITEMS));
		}
		
		return upperInput;
	}

	@Override
	public String unmarshal(Object o)
	{
		if (o instanceof String)
		{
			String item = ((String) o).toUpperCase();
			// Validate on unmarshal too (for config loading)
			if (BLOCKABLE_ITEMS.contains(item))
			{
				return item;
			}
		}
		return null;
	}

	public static Set<String> getBlockableItems()
	{
		return new HashSet<>(BLOCKABLE_ITEMS);
	}
}

