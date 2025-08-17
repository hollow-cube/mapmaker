package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

public record AxiomServerboundMarkerRequestPacket(
    UUID id
) implements ServerboundModPacket<AxiomServerboundMarkerRequestPacket> {

    public static final Type<AxiomServerboundMarkerRequestPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "marker_nbt_request",
            NetworkBufferTemplate.template(
                    NetworkBuffer.UUID, AxiomServerboundMarkerRequestPacket::id,
                    AxiomServerboundMarkerRequestPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundMarkerRequestPacket> getType() {
        return TYPE;
    }
}
