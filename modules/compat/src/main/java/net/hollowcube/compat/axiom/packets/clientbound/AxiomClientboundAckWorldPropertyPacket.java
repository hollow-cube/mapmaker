package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomClientboundAckWorldPropertyPacket(
    int sequence
) implements AxiomClientboundModPacket<AxiomClientboundAckWorldPropertyPacket> {

    public static final Type<AxiomClientboundAckWorldPropertyPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "ack_world_properties",
            NetworkBufferTemplate.template(
                    NetworkBuffer.VAR_INT, AxiomClientboundAckWorldPropertyPacket::sequence,
                    AxiomClientboundAckWorldPropertyPacket::new
            )
    );

    @Override
    public Type<AxiomClientboundAckWorldPropertyPacket> getType() {
        return TYPE;
    }
}
