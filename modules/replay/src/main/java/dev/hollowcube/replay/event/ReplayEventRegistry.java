package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.List;

public final class ReplayEventRegistry {

    public static Builder builder() {
        return new Builder();
    }

    private final Entry<?>[] idLookup;
    private final ClassValue<Entry<?>> typeLookup = new ClassValue<>() {
        @Override
        protected Entry<?> computeValue(Class<?> type) {
            for (var entry : idLookup) {
                if (entry.eventClass().equals(type)) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("unknown event type: " + type);
        }
    };

    private ReplayEventRegistry(List<Entry<?>> entries) {
        this.idLookup = entries.toArray(new Entry[0]);
    }

    public <T extends ReplayEvent> void write(NetworkBuffer buffer, T event) {
        //noinspection unchecked
        var entry = (Entry<T>) typeLookup.get(event.getClass());
        buffer.write(NetworkBuffer.VAR_INT, entry.id());
        entry.codec().write(buffer, event);
    }

    public ReplayEvent read(NetworkBuffer buffer, ChunkIndex chunk) {
        return entry(buffer).codec().read(buffer, chunk);
    }

    private Entry<?> entry(NetworkBuffer buffer) {
        int id = buffer.read(NetworkBuffer.VAR_INT);
        if (id < 0 || id >= idLookup.length)
            throw new IllegalArgumentException("invalid event id: " + id);
        return idLookup[id];
    }

    public static final class Builder {
        private final List<Entry<?>> events = new ArrayList<>();

        private Builder() {
        }

        /// For an event that carries no game data and whose type reads the bytes of every readable
        /// format version by itself.
        public <T extends ReplayEvent> Builder register(Class<T> eventClass, NetworkBuffer.Type<T> networkType) {
            return register(eventClass, ReplayEventCodec.of(networkType));
        }

        public <T extends ReplayEvent> Builder register(Class<T> eventClass, ReplayEventCodec<T> codec) {
            events.add(new Entry<>(eventClass, events.size(), codec));
            return this;
        }

        public ReplayEventRegistry build() {
            return new ReplayEventRegistry(events);
        }
    }

    record Entry<T extends ReplayEvent>(Class<T> eventClass, int id, ReplayEventCodec<T> codec) {}

}
