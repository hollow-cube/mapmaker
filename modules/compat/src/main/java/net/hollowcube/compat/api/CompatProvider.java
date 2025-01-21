package net.hollowcube.compat.api;

import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.hollowcube.compat.noxesium.NoxesiumCompatProvider;
import net.minestom.server.event.GlobalEventHandler;

import java.util.List;

/**
 * Provides compatibility with other mods or services.
 */
public interface CompatProvider {

    default void registerPackets(PacketRegistry registry) {
    }

    default void registerListeners(GlobalEventHandler events) {
    }

    static void load(GlobalEventHandler events) {
        List<CompatProvider> providers = List.of(
                new NoxesiumCompatProvider()
        );


        PacketRegistryImpl packets = PacketRegistryImpl.init(events);
        for (CompatProvider provider : providers) {
            provider.registerPackets(packets);
            provider.registerListeners(events);
        }
        packets.freeze();
    }

}
