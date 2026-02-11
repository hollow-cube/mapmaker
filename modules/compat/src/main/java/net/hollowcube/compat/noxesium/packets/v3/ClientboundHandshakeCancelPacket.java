package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.MagicConstant;

public record ClientboundHandshakeCancelPacket(
    @MagicConstant(valuesFromClass = ClientboundHandshakeCancelPacket.class) byte reason
) implements ClientboundModPacket<ClientboundHandshakeCancelPacket> {

    public static final byte NO_MATCHING_ENTRYPOINTS = 6;

    public static final Type<ClientboundHandshakeCancelPacket> TYPE = Type.of(
        "noxesium-v3", "clientbound_handshake_cancel-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.BYTE, ClientboundHandshakeCancelPacket::reason,
            ClientboundHandshakeCancelPacket::new
        )
    );

    @Override
    public Type<ClientboundHandshakeCancelPacket> getType() {
        return TYPE;
    }
}
