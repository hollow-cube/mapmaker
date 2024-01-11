package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.LONG;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientChunkDataRequestPacket(
        long correlationId
//    @NotNull String dimensionName,
//    boolean sendBlockEntities,

) implements AxiomClientPacket {

    public AxiomClientChunkDataRequestPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(LONG));
    }
}
