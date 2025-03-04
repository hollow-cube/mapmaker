package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomServerboundHelloPacket(
        int apiVersion,
        int dataVersion,
        int protocolVersion
) implements ServerboundModPacket<AxiomServerboundHelloPacket> {

    public static final Type<AxiomServerboundHelloPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "hello",
            NetworkBufferTemplate.template(
                    NetworkBuffer.VAR_INT, AxiomServerboundHelloPacket::apiVersion,
                    NetworkBuffer.VAR_INT, AxiomServerboundHelloPacket::dataVersion,
                    NetworkBuffer.VAR_INT, AxiomServerboundHelloPacket::protocolVersion,
                    AxiomServerboundHelloPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundHelloPacket> getType() {
        return TYPE;
    }
}
