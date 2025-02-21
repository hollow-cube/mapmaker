package net.hollowcube.compat.vanilla.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClientboundAddMarkerPacket(
        Point point,
        AlphaColor color,
        String label,
        int duration
) implements ClientboundModPacket<ClientboundAddMarkerPacket> {

    public static final Type<ClientboundAddMarkerPacket> TYPE = Type.of(
            "minecraft", "debug/game_test_add_marker",
            NetworkBufferTemplate.template(
                    NetworkBuffer.BLOCK_POSITION, ClientboundAddMarkerPacket::point,
                    NetworkBuffer.INT.transform(AlphaColor::new, AlphaColor::asARGB), ClientboundAddMarkerPacket::color,
                    NetworkBuffer.STRING, ClientboundAddMarkerPacket::label,
                    NetworkBuffer.INT, ClientboundAddMarkerPacket::duration,
                    ClientboundAddMarkerPacket::new
            )
    );

    @Override
    public Type<ClientboundAddMarkerPacket> getType() {
        return TYPE;
    }
}
