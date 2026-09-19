package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;

/// A block change the recording observed, whatever put it there.
public record SetBlockEvent(BlockVec position, Block block) implements ReplayEvent {
    public static final ReplayEventCodec<SetBlockEvent> CODEC = new ReplayEventCodec<>() {
        @Override
        public void write(NetworkBuffer buffer, SetBlockEvent event) {
            buffer.write(NetworkBuffer.BLOCK_POSITION, event.position());
            buffer.write(NetworkBuffer.STRING, event.block().state());
        }

        @Override
        public SetBlockEvent read(NetworkBuffer buffer, ChunkIndex chunk) {
            var position = buffer.read(NetworkBuffer.BLOCK_POSITION);
            return new SetBlockEvent(position, ReplayGameData.readBlock(buffer, chunk));
        }
    };

    /// The wire only ever carries a block position, so any point is taken as one rather than
    /// leaving two events that mean the same thing unequal to each other.
    public SetBlockEvent(Point position, Block block) {
        this(position.asBlockVec(), block);
    }
}
