package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomClientboundHelloPacket(
        long handshakeId
) implements ClientboundModPacket<AxiomClientboundHelloPacket> {

    public static final Type<AxiomClientboundHelloPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "hello",
            NetworkBufferTemplate.template(
                    NetworkBuffer.LONG, AxiomClientboundHelloPacket::handshakeId,
                    AxiomClientboundHelloPacket::new
            )
    );

    @Override
    public Type<AxiomClientboundHelloPacket> getType() {
        return TYPE;
    }
}
