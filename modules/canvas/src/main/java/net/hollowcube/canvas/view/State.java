package net.hollowcube.canvas.view;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Function;
import java.util.function.Supplier;

public interface State<T> {

    // Single value state

    static <T> @NotNull State<T> value(@NotNull String id, @NotNull Supplier<T> def) {
        record ValueState<T>(@NotNull String id, @NotNull Supplier<T> def) implements State<T> {
            @Override
            public @UnknownNullability T from(@NotNull Holder holder, @Nullable Supplier<T> def) {
                if (!holder.contains(id))
                    return (def == null ? this.def : def).get();
                return (T) holder.get(id);
            }
            @Override
            public void to(@NotNull Holder holder, @UnknownNullability T value, boolean markDirty) {
                holder.set(id, value, markDirty);
            }
        }
        return new ValueState<>(id, def);
    }

    static <T> @NotNull State<T> derived(@NotNull String id, Function<@NotNull Getter, T> get) {
        record DerivedState<T>(@NotNull String id, Function<@NotNull Getter, T> get) implements State<T>{
            @Override
            public @UnknownNullability T from(@NotNull Holder holder, @Nullable Supplier<T> def) {
                return get.apply(holder);
            }

            @Override
            public void to(@NotNull Holder holder, @UnknownNullability T value, boolean markDirty) {
                throw new UnsupportedOperationException();
            }
        }
        return new DerivedState<>(id, get);
    }

    @UnknownNullability T from(@NotNull Holder holder, @Nullable Supplier<T> def);
    void to(@NotNull Holder holder, @UnknownNullability T value, boolean markDirty);


    // Keyed state

    static <K, T> State.@NotNull Keyed<K, T> keyed(@NotNull String id, @NotNull Function<K, T> def) {
        record KeyedState<K, T>(@NotNull String id, @NotNull Function<K, T> def) implements State.Keyed<K, T> {
            @Override
            public @UnknownNullability T from(@NotNull Holder holder, @NotNull K key, @Nullable Function<K, T> def) {
                if (!holder.contains(id))
                    return (def == null ? this.def : def).apply(key);
                return (T) holder.get(id);
            }

            @Override
            public void to(@NotNull Holder holder, @NotNull K key, @UnknownNullability T value, boolean markDirty) {
                holder.set(id, value, markDirty);
            }
        }
        return new KeyedState<>(id, def);
    }

    interface Keyed<K, T> {
        @UnknownNullability T from(@NotNull Holder holder, @NotNull K key, @Nullable Function<K, T> def);
        void to(@NotNull Holder holder, @NotNull K key, @UnknownNullability T value, boolean markDirty);
    }

    // Get/Set utilities

    interface Getter {
        <T> @UnknownNullability T get(@NotNull State<T> state);
        <T> @UnknownNullability T get(@NotNull State<T> state, @NotNull Supplier<T> def);

        <K, T> @UnknownNullability T get(@NotNull State.Keyed<K, T> state, @NotNull K key);
    }

    interface Setter {
        <T> void set(@NotNull State<T> state, @UnknownNullability T value);
        <K, T> void set(@NotNull Keyed<K, T> state, @NotNull K key, @UnknownNullability T value);

        @ApiStatus.Internal
        <T> void unsafeSet(@NotNull State<T> state, @UnknownNullability T value);
        @ApiStatus.Internal
        <K, T> void unsafeSet(@NotNull State.Keyed<K, T> state, @NotNull K key, @UnknownNullability T value);
    }

    @ApiStatus.Internal
    interface Holder extends Getter, Setter {

        @Override
        default <T> @UnknownNullability T get(@NotNull State<T> state) {
            return state.from(this, null);
        }

        @Override
        default <T> @NotNull T get(@NotNull State<T> state, @NotNull Supplier<T> def) {
            return state.from(this, def);
        }

        @Override
        default <K, T> @UnknownNullability T get(@NotNull Keyed<K, T> state, @NotNull K key) {
            return state.from(this, key, null);
        }

        @Override
        default <T> void set(@NotNull State<T> state, @UnknownNullability T value) {
            state.to(this, value, true);
        }

        @Override
        default <K, T> void set(@NotNull Keyed<K, T> state, @NotNull K key, @UnknownNullability T value) {
            state.to(this, key, value, true);
        }

        @Override
        default <T> void unsafeSet(@NotNull State<T> state, @UnknownNullability T value) {
            state.to(this, value, false);
        }

        @Override
        default <K, T> void unsafeSet(@NotNull Keyed<K, T> state, @NotNull K key, @UnknownNullability T value) {
            state.to(this, key, value, false);
        }

        boolean contains(@NotNull String id);
        @UnknownNullability Object get(@NotNull String id);
        void set(@NotNull String id, @UnknownNullability Object value, boolean markDirty);
    }

}
