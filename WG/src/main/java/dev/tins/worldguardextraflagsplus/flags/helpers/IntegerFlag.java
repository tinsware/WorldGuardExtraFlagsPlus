package dev.tins.worldguardextraflagsplus.flags.helpers;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

public class IntegerFlag extends Flag<String>
{
	public IntegerFlag(String name)
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

		if (input.isEmpty())
		{
			throw new InvalidFlagFormat("Flag value cannot be empty. Expected a valid integer (e.g., 10)");
		}

		// Validate that input is a valid integer
		try
		{
			Integer.parseInt(input);
			return input;
		}
		catch (NumberFormatException e)
		{
			throw new InvalidFlagFormat("Expected a valid integer. Got: '" + input + "'");
		}
	}

	@Override
	public String unmarshal(Object o)
	{
		return o.toString();
	}
}
