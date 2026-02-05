package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.noxesium.components.NoxesiumComponentMap;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClientboundUpdateGameComponentsPacket(
    boolean reset,
    NoxesiumComponentMap components
) implements ClientboundModPacket<ClientboundUpdateGameComponentsPacket> {

    public static final ClientboundModPacket.Type<ClientboundUpdateGameComponentsPacket> TYPE = ClientboundModPacket.Type.of(
        "noxesium-v3", "clientbound_update_game_components-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.BOOLEAN, ClientboundUpdateGameComponentsPacket::reset,
            NoxesiumComponentMap.NETWORK_TYPE, ClientboundUpdateGameComponentsPacket::components,
            ClientboundUpdateGameComponentsPacket::new
        )
    );

    @Override
    public ClientboundModPacket.Type<ClientboundUpdateGameComponentsPacket> getType() {
        return TYPE;
    }
}