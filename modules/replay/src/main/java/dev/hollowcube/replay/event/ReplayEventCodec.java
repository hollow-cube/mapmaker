package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import net.minestom.server.network.NetworkBuffer;

/// For an event carrying game data, which is read against the data version of the chunk holding it
/// so it can be upgraded. Anything else registers a plain [NetworkBuffer.Type].
public interface ReplayEventCodec<T extends ReplayEvent> {

    void write(NetworkBuffer buffer, T event);

    T read(NetworkBuffer buffer, ChunkIndex chunk);

    static <T extends ReplayEvent> ReplayEventCodec<T> of(NetworkBuffer.Type<T> type) {
        return new ReplayEventCodec<>() {
            @Override
            public void write(NetworkBuffer buffer, T event) {
                buffer.write(type, event);
            }

            @Override
            public T read(NetworkBuffer buffer, ChunkIndex chunk) {
                return buffer.read(type);
            }
        };
    }
}
