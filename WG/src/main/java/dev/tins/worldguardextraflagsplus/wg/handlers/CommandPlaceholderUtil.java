package dev.tins.worldguardextraflagsplus.wg.handlers;

import com.sk89q.worldguard.LocalPlayer;

final class CommandPlaceholderUtil
{
	private CommandPlaceholderUtil()
	{
	}

	/**
	 * Strips a leading {@code /} when present (manual region YAML often omits it; WG CLI adds it).
	 * Applies {@code %player%}, {@code %username%}, {@code {player}}, {@code {username}} placeholders.
	 */
	static String prepareForDispatch(LocalPlayer player, String rawCommand)
	{
		if (rawCommand == null)
		{
			return "";
		}
		String c = rawCommand.trim();
		if (c.startsWith("/"))
		{
			c = c.substring(1);
		}
		return c.replace("%username%", player.getName())
			.replace("%player%", player.getName())
			.replace("{player}", player.getName())
			.replace("{username}", player.getName());
	}
}
