package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record AxiomChunkDataResponsePacket(
        long id
        //todo
) implements AxiomServerPacket {
    @Override
    public @NotNull String packetChannel() {
        return "axiom:response_chunk_data";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.write(NetworkBuffer.LONG, id);
        buffer.write(NetworkBuffer.LONG, ProtocolUtil.MIN_POSITION_LONG); // No block entities yet
        buffer.write(NetworkBuffer.LONG, ProtocolUtil.MIN_POSITION_LONG); // No chunks yet
        buffer.write(NetworkBuffer.BOOLEAN, true); // Finished
    }
}
