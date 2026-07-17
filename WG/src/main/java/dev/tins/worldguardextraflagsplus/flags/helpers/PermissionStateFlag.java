package dev.tins.worldguardextraflagsplus.flags.helpers;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

/**
 * Custom flag type for permission-based entry control.
 * Accepts permission strings like "myPlugin.permission.1" or "*" for all permissions.
 */
public class PermissionStateFlag extends Flag<String>
{
	public PermissionStateFlag(String name)
	{
		super(name);
	}

	@Override
	public String parseInput(FlagContext context) throws InvalidFlagFormat
	{
		String input = context.getUserInput().trim();
		
		if (input.isEmpty())
		{
			throw new InvalidFlagFormat("Permission cannot be empty. Format: permission.node (e.g., myPlugin.enter.premium)");
		}
		
		// Validate basic permission format - alphanumeric, dots, hyphens, underscores, and wildcards
		if (!input.matches("^[a-zA-Z0-9._*-]+$"))
		{
			throw new InvalidFlagFormat("Invalid permission format: '" + input + "'. Use alphanumeric characters, dots, hyphens, and underscores. Wildcards (*) are allowed.");
		}
		
		return input;
	}

	@Override
	public Object marshal(String o)
	{
		return o;
	}

	@Override
    public String unmarshal(Object o) {
        if (o == null) {
            return null;
        }
        return o.toString();
    }
}
