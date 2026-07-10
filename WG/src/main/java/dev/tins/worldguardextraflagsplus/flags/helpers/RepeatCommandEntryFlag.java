package dev.tins.worldguardextraflagsplus.flags.helpers;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;

/**
 * Element flag used inside {@code CustomSetFlag} for {@code console-command-repeat}.
 *
 * <p>Unlike {@link CommandStringCaseSensitiveFlag}, this flag does <b>not</b> prepend a
 * {@code /} to the input — the value format is {@code <seconds> <command>} where the
 * command is dispatched from the console as-is.</p>
 */
public class RepeatCommandEntryFlag extends Flag<String>
{
	public RepeatCommandEntryFlag(String name)
	{
		super(name);
	}

	@Override
	public String parseInput(FlagContext context)
	{
		return context.getUserInput().trim();
	}

	@Override
	public Object marshal(String o)
	{
		return o;
	}

	@Override
	public String unmarshal(Object o)
	{
		return o instanceof String string ? string : null;
	}
}
