package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetBlockPacket(
        Point blockPosition,
        Block block,
        boolean updateNeighbors,
        int sequence
) implements AxiomClientPacket {

    public AxiomClientSetBlockPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(BLOCK_POSITION),
                Block.fromStateId((short) buffer.read(BLOCK_STATE).intValue()),
                buffer.read(BOOLEAN),
                buffer.read(INT));
    }

}
