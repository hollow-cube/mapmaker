package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.Map;

public record ClientboundRegistryIdsUpdatePacket(
    int id,
    boolean reset,
    Key registry,
    Map<Key, Integer> ids
) implements ClientboundModPacket<ClientboundRegistryIdsUpdatePacket> {

    public static final ClientboundModPacket.Type<ClientboundRegistryIdsUpdatePacket> TYPE = ClientboundModPacket.Type.of(
        "noxesium-v3", "clientbound_registry_update_ids-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.VAR_INT, ClientboundRegistryIdsUpdatePacket::id,
            NetworkBuffer.BOOLEAN, ClientboundRegistryIdsUpdatePacket::reset,
            NetworkBuffer.KEY, ClientboundRegistryIdsUpdatePacket::registry,
            NetworkBuffer.KEY.mapValue(NetworkBuffer.VAR_INT), ClientboundRegistryIdsUpdatePacket::ids,
            ClientboundRegistryIdsUpdatePacket::new
        )
    );

    @Override
    public ClientboundModPacket.Type<ClientboundRegistryIdsUpdatePacket> getType() {
        return TYPE;
    }
}
