package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClientboundHandshakeCompletePacket() implements ClientboundModPacket<ClientboundHandshakeCompletePacket> {

    public static final Type<ClientboundHandshakeCompletePacket> TYPE = Type.of(
        "noxesium-v3", "clientbound_handshake_complete-p1",
        NetworkBufferTemplate.template(
            ClientboundHandshakeCompletePacket::new
        )
    );

    @Override
    public Type<ClientboundHandshakeCompletePacket> getType() {
        return TYPE;
    }
}
