package dev.tins.worldguardextraflagsplus.protocollib;

import org.bukkit.plugin.Plugin;

import lombok.Getter;
import dev.tins.worldguardextraflagsplus.WorldGuardExtraFlagsPlusPlugin;

/**
 * Registers ProtocolLib listeners via reflection only — no {@code com.comphenix} imports so this class
 * loads when ProtocolLib is absent (optional softdepend).
 */
public class ProtocolLibHelper
{
	@Getter private final WorldGuardExtraFlagsPlusPlugin plugin;
	@Getter private final Plugin protocolLibPlugin;

	public ProtocolLibHelper(WorldGuardExtraFlagsPlusPlugin plugin, Plugin protocolLibPlugin)
	{
		this.plugin = plugin;
		this.protocolLibPlugin = protocolLibPlugin;
	}

	public void onEnable()
	{
		try
		{
			ClassLoader cl = this.plugin.getClass().getClassLoader();
			Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary", true, cl);
			Object protocolManager = protocolLibraryClass.getMethod("getProtocolManager").invoke(null);
			Class<?> listenerClass = Class.forName(
					"dev.tins.worldguardextraflagsplus.protocollib.RemoveEffectPacketListener",
					true,
					cl);
			Object listener = listenerClass.getConstructor().newInstance();
			Class<?> packetListenerClass = Class.forName("com.comphenix.protocol.events.PacketListener", true, cl);
			protocolManager.getClass().getMethod("addPacketListener", packetListenerClass).invoke(protocolManager,
					listener);
		}
		catch (Throwable t)
		{
			this.plugin.getLogger().warning("[give-effects] Failed to register ProtocolLib packet listener: "
					+ (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
		}
	}
}
