package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record AxiomClientChunkDataRequestPacket(
        long correlationId,
        @NotNull String worldName,
        boolean sendBlockEntitiesInChunks,
        long[] blockEntities,
        long[] chunkSections
) implements AxiomClientPacket {
    public static final @NotNull NetworkBuffer.Type<AxiomClientChunkDataRequestPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.LONG, AxiomClientChunkDataRequestPacket::correlationId,
            NetworkBuffer.STRING, AxiomClientChunkDataRequestPacket::worldName,
            NetworkBuffer.BOOLEAN, AxiomClientChunkDataRequestPacket::sendBlockEntitiesInChunks,
            NetworkBuffer.LONG_ARRAY, AxiomClientChunkDataRequestPacket::blockEntities,
            NetworkBuffer.LONG_ARRAY, AxiomClientChunkDataRequestPacket::chunkSections,
            AxiomClientChunkDataRequestPacket::new);
}
