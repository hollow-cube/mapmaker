package net.hollowcube.compat.noxesium.components;

import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record NoxesiumComponentType<T>(
    int networkId,
    Key key,
    NetworkBuffer.Type<@NotNull T> networkType
) {

    public interface Holder {

        default boolean has(NoxesiumComponentType<?> type) {
            return this.get(type) != null;
        }

        <T> @Nullable T get(NoxesiumComponentType<T> type);

        default void set(NoxesiumComponentType<Unit> type, boolean value) {
            this.set(type, value ? Unit.INSTANCE : null);
        }

        <T> void set(NoxesiumComponentType<T> type, @Nullable T value);

        void clear();
    }
}
