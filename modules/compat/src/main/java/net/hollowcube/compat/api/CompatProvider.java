package net.hollowcube.compat.api;

import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.minestom.server.event.GlobalEventHandler;

import java.util.ServiceLoader;

/**
 * Provides compatibility with other mods or services.
 */
public interface CompatProvider {

    default void registerPackets(PacketRegistry registry) {
    }

    default void registerListeners(GlobalEventHandler events) {
    }

    static void load(GlobalEventHandler events) {
        PacketRegistryImpl packets = PacketRegistryImpl.init(events);
        for (CompatProvider provider : ServiceLoader.load(CompatProvider.class)) {
            provider.registerPackets(packets);
            provider.registerListeners(events);
        }
        packets.freeze();
    }

}
