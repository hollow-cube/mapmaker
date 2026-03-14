package net.hollowcube.compat.noxesium.components;

import net.hollowcube.common.util.NetworkBufferTypes;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class NoxesiumComponentMap implements NoxesiumComponentType.Holder {

    public static final NetworkBuffer.Type<NoxesiumComponentMap> NETWORK_TYPE = NetworkBufferTypes.writeOnly(
        (buffer, components) -> {
            // We don't support removing components we send the entire map if we need to update it since we only send them on state changes.
            // So the second var int is always 0
            NetworkBuffer.VAR_INT.write(buffer, components.components.size());
            NetworkBuffer.VAR_INT.write(buffer, 0);

            components.forEach(new ForEachConsumer() {
                @Override
                public <T> void accept(NoxesiumComponentType<T> type, T value) {
                    NetworkBuffer.VAR_INT.write(buffer, type.networkId());
                    type.networkType().write(buffer, value);
                }
            });
        }
    );

    private final Map<NoxesiumComponentType<?>, Object> components = new HashMap<>();

    @Override
    public <T> @Nullable T get(NoxesiumComponentType<T> type) {
        return (T) this.components.get(type);
    }

    @Override
    public <T> void set(NoxesiumComponentType<T> type, @Nullable T value) {
        if (value == null) {
            this.components.remove(type);
        } else {
            this.components.put(type, value);
        }
    }

    @Override
    public void clear() {
        this.components.clear();
    }

    public void forEach(ForEachConsumer consumer) {
        this.components.forEach(
            (type, value) -> consumer.accept((NoxesiumComponentType<Object>) type, value)
        );
    }

    public NoxesiumComponentMap copy() {
        NoxesiumComponentMap copy = new NoxesiumComponentMap();
        copy.components.putAll(this.components);
        return copy;
    }

    @FunctionalInterface
    public interface ForEachConsumer {
        <T> void accept(NoxesiumComponentType<T> type, T value);
    }
}
