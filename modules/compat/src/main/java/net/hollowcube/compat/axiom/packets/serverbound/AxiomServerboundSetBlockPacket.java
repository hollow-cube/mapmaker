package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.AxiomSetBlockFlag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public record AxiomServerboundSetBlockPacket(
        @NotNull Map<Point, Block> blocks,
        @Nullable Set<Point> updateNeighbors,
        @MagicConstant(valuesFromClass = AxiomSetBlockFlag.class) int reason,
        boolean breaking,

        @NotNull Point pos,
        @NotNull BlockFace face,
        @NotNull Point cursor,
        boolean inside,

        @NotNull PlayerHand hand,
        int sequence
) implements ServerboundModPacket<AxiomServerboundSetBlockPacket> {

    public static final Type<AxiomServerboundSetBlockPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "set_block",
            NetworkBufferTemplate.template(
                    NetworkBuffer.BLOCK_POSITION.mapValue(Block.NETWORK_TYPE), AxiomServerboundSetBlockPacket::blocks,
                    NetworkBuffer.BLOCK_POSITION.set().optional(), AxiomServerboundSetBlockPacket::updateNeighbors,
                    NetworkBuffer.VAR_INT, AxiomServerboundSetBlockPacket::reason,
                    NetworkBuffer.BOOLEAN, AxiomServerboundSetBlockPacket::breaking,

                    NetworkBuffer.BLOCK_POSITION, AxiomServerboundSetBlockPacket::pos,
                    NetworkBuffer.Enum(BlockFace.class), AxiomServerboundSetBlockPacket::face,
                    NetworkBuffer.VECTOR3, AxiomServerboundSetBlockPacket::cursor,
                    NetworkBuffer.BOOLEAN, AxiomServerboundSetBlockPacket::inside,

                    NetworkBuffer.Enum(PlayerHand.class), AxiomServerboundSetBlockPacket::hand,
                    NetworkBuffer.VAR_INT, AxiomServerboundSetBlockPacket::sequence,

                    AxiomServerboundSetBlockPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundSetBlockPacket> getType() {
        return TYPE;
    }
}
