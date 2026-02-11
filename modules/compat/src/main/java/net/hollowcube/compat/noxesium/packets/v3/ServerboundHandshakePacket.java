package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.Map;

public record ServerboundHandshakePacket(
    Map<String, String> entrypoints
) implements ServerboundModPacket<ServerboundHandshakePacket> {

    public static final Type<ServerboundHandshakePacket> TYPE = Type.of(
        "noxesium-v3", "serverbound_handshake-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.STRING.mapValue(NetworkBuffer.STRING), ServerboundHandshakePacket::entrypoints,
            ServerboundHandshakePacket::new
        )
    );

    @Override
    public Type<ServerboundHandshakePacket> getType() {
        return TYPE;
    }
}
