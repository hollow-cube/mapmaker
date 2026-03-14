package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.buffers.AxiomBiomeBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBuffer;
import net.minestom.server.network.NetworkBuffer;

import java.util.UUID;

public record AxiomServerboundSetBufferPacket(
    String dimension,
    UUID id,
    AxiomBuffer buffer
) implements ServerboundModPacket<AxiomServerboundSetBufferPacket> {

    public static final Type<AxiomServerboundSetBufferPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "set_buffer",
        (buffer) -> new AxiomServerboundSetBufferPacket(
            buffer.read(NetworkBuffer.STRING),
            buffer.read(NetworkBuffer.UUID),
            switch (buffer.read(NetworkBuffer.BYTE)) {
                case 0 -> AxiomBlockBuffer.read(buffer);
                case 1 -> AxiomBiomeBuffer.read(buffer);
                default -> throw new IllegalArgumentException("Unknown buffer type");
            }
        )
    );

    @Override
    public Type<AxiomServerboundSetBufferPacket> getType() {
        return TYPE;
    }
}
