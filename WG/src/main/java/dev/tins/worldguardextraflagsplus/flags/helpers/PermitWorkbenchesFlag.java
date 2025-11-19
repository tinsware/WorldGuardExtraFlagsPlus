package dev.tins.worldguardextraflagsplus.flags.helpers;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

import java.util.HashSet;
import java.util.Set;

public class PermitWorkbenchesFlag extends Flag<String>
{
	// Valid workbench types that can be blocked
	private static final Set<String> VALID_WORKBENCHES = new HashSet<>();
	static
	{
		VALID_WORKBENCHES.add("ALL");
		VALID_WORKBENCHES.add("CLEAR");
		VALID_WORKBENCHES.add("ANVIL");
		VALID_WORKBENCHES.add("CARTOGRAPHY");
		VALID_WORKBENCHES.add("CRAFT");
		VALID_WORKBENCHES.add("ENDER");
		VALID_WORKBENCHES.add("GRINDSTONE");
		VALID_WORKBENCHES.add("LOOM");
		VALID_WORKBENCHES.add("SMITHING");
		VALID_WORKBENCHES.add("STONECUTTER");
	}

	public PermitWorkbenchesFlag(String name)
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
			throw new InvalidFlagFormat("Workbench type cannot be empty");
		}
		
		// Convert to uppercase for comparison
		String upperInput = input.toUpperCase();
		
		// Validate against valid list
		if (!VALID_WORKBENCHES.contains(upperInput))
		{
			throw new InvalidFlagFormat("Invalid workbench type '" + input + "'. Valid types: " + String.join(", ", VALID_WORKBENCHES));
		}
		
		return upperInput;
	}

	@Override
	public String unmarshal(Object o)
	{
		if (o instanceof String)
		{
			String workbench = ((String) o).toUpperCase();
			// Validate on unmarshal too (for config loading)
			if (VALID_WORKBENCHES.contains(workbench))
			{
				return workbench;
			}
		}
		return null;
	}

	public static Set<String> getValidWorkbenches()
	{
		return new HashSet<>(VALID_WORKBENCHES);
	}
}

