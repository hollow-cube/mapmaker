package net.hollowcube.terraform.compat.axiom.world.property;

import net.minestom.server.entity.Player;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public sealed interface WorldProperty<T> permits WorldPropertyImpl {

    static <T> @NotNull WorldProperty<T> global(
            @NotNull NamespaceID id,
            @NotNull String name,
            boolean localizeName,
            @NotNull WidgetType<T> type,
            @NotNull Handler<T> handler,
            @Nullable T initialValue
    ) {
        return new WorldPropertyImpl.Global<>(id, name, localizeName, type, handler, initialValue);
    }

    @NotNull NamespaceID id();

    @NotNull String name();

    boolean localizeName();

    @NotNull WidgetType<T> type();

    @UnknownNullability T getValue();

    @UnknownNullability T getValue(@NotNull Player player);

    void setValue(@NotNull T value, boolean sync);

    void setValue(@NotNull Player player, @NotNull T value, boolean sync);

    default void setValue(@NotNull T value) {
        setValue(value, true);
    }

    default void setValue(@NotNull Player player, @NotNull T value) {
        setValue(player, value, true);
    }

    @FunctionalInterface
    interface Handler<T> {
        boolean update(@NotNull WorldProperty<T> property, @NotNull Player player, @NotNull T newValue);
    }

}
