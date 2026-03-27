package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.Map;

public record ClientboundHandshakeAcknowledgePacket(
    Map<String, String> entrypoints
) implements ClientboundModPacket<ClientboundHandshakeAcknowledgePacket> {

    public static final ClientboundModPacket.Type<ClientboundHandshakeAcknowledgePacket> TYPE = ClientboundModPacket.Type.of(
        "noxesium-v3", "clientbound_handshake_ack-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.STRING.mapValue(NetworkBuffer.STRING), ClientboundHandshakeAcknowledgePacket::entrypoints,
            ClientboundHandshakeAcknowledgePacket::new
        )
    );

    @Override
    public ClientboundModPacket.Type<ClientboundHandshakeAcknowledgePacket> getType() {
        return TYPE;
    }
}
