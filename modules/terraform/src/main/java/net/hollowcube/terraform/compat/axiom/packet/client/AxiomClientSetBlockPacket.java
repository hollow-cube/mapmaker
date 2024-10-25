package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.common.util.NetworkBufferTypes;
import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record AxiomClientSetBlockPacket(
        @NotNull Map<Point, Block> blocks,
        boolean updateNeighbors,
        int reason,
        boolean breaking,
        @NotNull BlockHitResult blockHit,
        @NotNull PlayerHand hand,
        int sequence
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSetBlockPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.BLOCK_POSITION.mapValue(Block.NETWORK_TYPE), AxiomClientSetBlockPacket::blocks,
            NetworkBuffer.BOOLEAN, AxiomClientSetBlockPacket::updateNeighbors,
            NetworkBuffer.VAR_INT, AxiomClientSetBlockPacket::reason,
            NetworkBuffer.BOOLEAN, AxiomClientSetBlockPacket::breaking,
            BlockHitResult.SERIALIZER, AxiomClientSetBlockPacket::blockHit,
            NetworkBuffer.Enum(PlayerHand.class), AxiomClientSetBlockPacket::hand,
            NetworkBuffer.VAR_INT, AxiomClientSetBlockPacket::sequence,
            AxiomClientSetBlockPacket::new);

    public static final int REASON_REPLACEMODE = 1;
    public static final int REASON_TINKER = 2;
    public static final int REASON_FORCEPLACE = 4;
    public static final int REASON_NOUPDATES = 8;
    public static final int REASON_CUSTOMSHAPEUPDATE = 16;
    public static final int REASON_CUSTOMPLACEMENT = 32;
    public static final int REASON_INFINITEREACH = 64;
    public static final int REASON_ANGEL = 128;
    public static final int REASON_SYMMETRY = 256;

    public record BlockHitResult(
            @NotNull Point blockPos,
            @NotNull BlockFace blockFace,
            @NotNull Point cursorPosition,
            boolean inside
    ) {
        public static final NetworkBuffer.Type<BlockHitResult> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.BLOCK_POSITION, BlockHitResult::blockPos,
                NetworkBufferTypes.BLOCK_FACE, BlockHitResult::blockFace,
                NetworkBuffer.VECTOR3, BlockHitResult::cursorPosition,
                NetworkBuffer.BOOLEAN, BlockHitResult::inside,
                BlockHitResult::new);
    }

    public AxiomClientSetBlockPacket {
        blocks = Map.copyOf(blocks);
    }

}
