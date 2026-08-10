package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// A block change the recording observed, whatever put it there.
public record SetBlockEvent(BlockVec position, Block block) implements ReplayEvent {
    public static final NetworkBuffer.Type<SetBlockEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.BLOCK_POSITION, SetBlockEvent::position,
        Block.STATE_NETWORK_TYPE, SetBlockEvent::block,
        SetBlockEvent::new
    );

    /// The wire only ever carries a block position, so any point is taken as one rather than
    /// leaving two events that mean the same thing unequal to each other.
    public SetBlockEvent(Point position, Block block) {
        this(new BlockVec(position), block);
    }
}
