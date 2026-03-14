package net.hollowcube.compat.noxesium.components;

import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.hollowcube.compat.noxesium.packets.v3.ClientboundRegistryIdsUpdatePacket;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NoxesiumComponentRegistry {

    // We share the network IDs across all registries to make it easier to manage them, as to store less information about each registry sent.
    private static final AtomicInteger COMPONENT_ID = new AtomicInteger();
    private static final AtomicInteger REGISTRY_ID = new AtomicInteger();

    private final List<NoxesiumComponentType<?>> components = new ArrayList<>();
    private final Key key;
    private final int id;

    public NoxesiumComponentRegistry(String id) {
        this.key = Key.key(NoxesiumAPI.NAMESPACE, id);
        this.id = REGISTRY_ID.getAndIncrement();
    }

    public <T> NoxesiumComponentType<T> register(String id, NetworkBuffer.Type<T> networkType) {
        var component = new NoxesiumComponentType<>(COMPONENT_ID.getAndIncrement(), Key.key(NoxesiumAPI.NAMESPACE, id), networkType);
        this.components.add(component);
        return component;
    }

    public ClientboundRegistryIdsUpdatePacket toPacket() {
        var ids = new HashMap<Key, Integer>(this.components.size());
        for (var component : this.components) {
            ids.put(component.key(), component.networkId());
        }
        return new ClientboundRegistryIdsUpdatePacket(this.id, true, this.key, ids);
    }
}
