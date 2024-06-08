package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetBlockPacket(
        Map<Point, Block> blocks,
        boolean updateNeighbors,
        int reason,
        boolean breaking,
        BlockHitResult blockHit,
        Player.Hand hand,
        int sequence
) implements AxiomClientPacket {

    public record BlockHitResult(
            @NotNull Point blockPos,
            @NotNull BlockFace blockFace,
            @NotNull Point cursorPosition,
            boolean inside
    ) {
        public BlockHitResult(@NotNull NetworkBuffer buffer) {
            this(buffer.read(BLOCK_POSITION), BlockFace.fromDirection(buffer.read(DIRECTION)), buffer.read(VECTOR3), buffer.read(BOOLEAN));
        }
    }

    public AxiomClientSetBlockPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(
                ProtocolUtil.readMap(buffer,
                        b -> b.read(BLOCK_POSITION),
                        b -> b.read(Block.NETWORK_TYPE)),
                buffer.read(BOOLEAN),
                buffer.read(VAR_INT),
                buffer.read(BOOLEAN),
                new BlockHitResult(buffer),
                buffer.readEnum(Player.Hand.class),
                buffer.read(VAR_INT)
        );
    }

}
