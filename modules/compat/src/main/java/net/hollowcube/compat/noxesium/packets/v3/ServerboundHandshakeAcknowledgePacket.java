package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.List;

public record ServerboundHandshakeAcknowledgePacket(
    List<Entrypoint> entrypoints,
    List<Mod> mods
) implements ServerboundModPacket<ServerboundHandshakeAcknowledgePacket> {

    public static final Type<ServerboundHandshakeAcknowledgePacket> TYPE = Type.of(
        "noxesium-v3", "serverbound_handshake_ack-p1",
        NetworkBufferTemplate.template(
            Entrypoint.TYPE.list(), ServerboundHandshakeAcknowledgePacket::entrypoints,
            Mod.TYPE.list(), ServerboundHandshakeAcknowledgePacket::mods,
            ServerboundHandshakeAcknowledgePacket::new
        )
    );

    @Override
    public Type<ServerboundHandshakeAcknowledgePacket> getType() {
        return TYPE;
    }

    public record Mod(
        String id,
        String version
    ) {

        public static final NetworkBuffer.Type<Mod> TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, Mod::id,
            NetworkBuffer.STRING, Mod::version,
            Mod::new
        );
    }

    public record Entrypoint(
        String id,
        String version,
        List<Key> capabilities
    ) {

        public static final NetworkBuffer.Type<Entrypoint> TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, Entrypoint::id,
            NetworkBuffer.STRING, Entrypoint::version,
            NetworkBuffer.KEY.list(), Entrypoint::capabilities,
            Entrypoint::new
        );
    }
}