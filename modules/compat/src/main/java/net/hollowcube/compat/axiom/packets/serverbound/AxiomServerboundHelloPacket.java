package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;

public record AxiomServerboundHelloPacket(
        int apiVersion,
        int dataVersion,
        int protocolVersion,
        long handshakeId
) implements ServerboundModPacket<AxiomServerboundHelloPacket> {

    public static final Type<AxiomServerboundHelloPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "hello",
            buffer -> {
                int apiVersion = buffer.read(NetworkBuffer.VAR_INT);
                return switch (apiVersion) {
                    case AxiomAPI.API_9 -> new AxiomServerboundHelloPacket(apiVersion,
                            buffer.read(NetworkBuffer.VAR_INT), buffer.read(NetworkBuffer.VAR_INT), 0);
                    case AxiomAPI.API_10 -> new AxiomServerboundHelloPacket(apiVersion,
                            buffer.read(NetworkBuffer.VAR_INT), buffer.read(NetworkBuffer.VAR_INT), buffer.read(NetworkBuffer.LONG));
                    default -> {
                        // Unknown APIs own the rest of the payload; the handler rejects on the version alone.
                        buffer.readIndex(buffer.writeIndex());
                        yield new AxiomServerboundHelloPacket(apiVersion, 0, 0, 0);
                    }
                };
            }
    );

    @Override
    public Type<AxiomServerboundHelloPacket> getType() {
        return TYPE;
    }
}
