package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.buffers.AxiomBiomeBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBuffer;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record AxiomServerboundSetBufferPacket(
        @NotNull String dimension,
        @NotNull UUID id,
        @NotNull AxiomBuffer buffer,
        int clientAvailableDispatchSends
) implements ServerboundModPacket<AxiomServerboundSetBufferPacket> {

    public static final Type<AxiomServerboundSetBufferPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "set_buffer",
            (buffer) -> {
                var dimension = buffer.read(NetworkBuffer.STRING);
                var id = buffer.read(NetworkBuffer.UUID);
                AxiomBuffer contents = switch (buffer.read(NetworkBuffer.BYTE)) {
                    case 0 -> AxiomBlockBuffer.read(buffer);
                    case 1 -> AxiomBiomeBuffer.read(buffer);
                    default -> throw new IllegalArgumentException("Unknown buffer type");
                };
                return new AxiomServerboundSetBufferPacket(dimension, id, contents, buffer.read(NetworkBuffer.VAR_INT));
            }
    );

    @Override
    public Type<AxiomServerboundSetBufferPacket> getType() {
        return TYPE;
    }
}
