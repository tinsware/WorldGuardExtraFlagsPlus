package dev.tins.worldguardextraflagsplus.util;

import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;

/**
 * Centralized plugin logo display.
 */
public final class Logo {

    private Logo() {
    }

    /**
     * Display the plugin logo.
     * Logo lines are logged without prefix; ANSI color codes are embedded in each line.
     */
    public static void display() {
        WorldGuardExtraFlagsPlusPlugin plugin = WorldGuardExtraFlagsPlusPlugin.getPlugin();
        if (plugin == null) {
            return;
        }

        String reset = "\u001B[0m";
        String red = "\u001B[31m";
        String orange = "\u001B[38;5;208m";

        plugin.getServer().getConsoleSender().sendMessage("");
        plugin.getServer().getConsoleSender().sendMessage(red + "ＴＩＮＳ  ＭＣ  （ｔｉｎｓｗａｒｅ）" + reset);
        plugin.getServer().getConsoleSender().sendMessage(orange + "┓ ┏┏┓┏┓┏┓┏┓" + reset);
        plugin.getServer().getConsoleSender().sendMessage(orange + "┃┃┃┃┓┣ ┣ ┃┃" + reset);
        plugin.getServer().getConsoleSender().sendMessage(orange + "┗┻┛┗┛┗┛┻ ┣┛" + reset);
        plugin.getServer().getConsoleSender().sendMessage("");
    }
}
