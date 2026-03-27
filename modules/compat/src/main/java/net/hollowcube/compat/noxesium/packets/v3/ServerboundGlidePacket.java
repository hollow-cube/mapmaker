package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ServerboundGlidePacket(
    boolean isGliding
) implements ServerboundModPacket<ServerboundGlidePacket> {

    public static final Type<ServerboundGlidePacket> TYPE = Type.of(
        "noxesium-v3", "serverbound_glide-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.BOOLEAN, ServerboundGlidePacket::isGliding,
            ServerboundGlidePacket::new
        )
    );

    @Override
    public Type<ServerboundGlidePacket> getType() {
        return TYPE;
    }
}
