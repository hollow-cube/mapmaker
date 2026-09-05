package dev.hollowcube.replay.event;

import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
        buffer.write(entry.networkType(), event);
    }

    public ReplayEvent read(NetworkBuffer buffer) {
        int id = buffer.read(NetworkBuffer.VAR_INT);
        if (id < 0 || id >= idLookup.length)
            throw new IllegalArgumentException("invalid event id: " + id);
        var entry = idLookup[id];
        return buffer.read(entry.networkType());
    }

    /// Advances over an event; a custom skipper can avoid resolving host game state.
    public void skip(NetworkBuffer buffer) {
        int id = buffer.read(NetworkBuffer.VAR_INT);
        if (id < 0 || id >= idLookup.length)
            throw new IllegalArgumentException("invalid event id: " + id);
        idLookup[id].skip().accept(buffer);
    }

    public static final class Builder {
        private final List<Entry<?>> events = new ArrayList<>();

        private Builder() {
        }

        public <T extends ReplayEvent> Builder register(Class<T> eventClass, NetworkBuffer.Type<T> networkType) {
            return register(eventClass, networkType, networkType::read);
        }

        public <T extends ReplayEvent> Builder register(Class<T> eventClass, NetworkBuffer.Type<T> networkType,
                                                        Consumer<NetworkBuffer> skip) {
            events.add(new Entry<>(eventClass, events.size(), networkType, skip));
            return this;
        }

        public ReplayEventRegistry build() {
            return new ReplayEventRegistry(events);
        }
    }

    record Entry<T extends ReplayEvent>(Class<T> eventClass, int id, NetworkBuffer.Type<T> networkType,
                                        Consumer<NetworkBuffer> skip) {}

}
