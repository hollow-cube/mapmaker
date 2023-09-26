package net.hollowcube.terraform.compat.axiom.world.property;

import net.minestom.server.entity.Player;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

sealed abstract class WorldPropertyImpl<T> implements WorldProperty<T> {
    private final NamespaceID id;
    private final String name;
    private final boolean localizeName;
    private final WidgetType<T> type;
    private final Handler<T> handler;

    WorldPropertyImpl(
            @NotNull NamespaceID id,
            @NotNull String name,
            boolean localizeName,
            @NotNull WidgetType<T> type,
            @NotNull Handler<T> handler
    ) {
        this.id = id;
        this.name = name;
        this.localizeName = localizeName;
        this.type = type;
        this.handler = handler;
    }

    @Override
    public @NotNull NamespaceID id() {
        return id;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean localizeName() {
        return localizeName;
    }

    @Override
    public @NotNull WidgetType<T> type() {
        return type;
    }

    public void update(@NotNull Player player, T value) {
        if (handler.update(this, player, value)) {
            setValue(player, value, true);
        }
    }

    static final class Global<T> extends WorldPropertyImpl<T> {
        private T value;

        Global(@NotNull NamespaceID id, @NotNull String name, boolean localizeName, @NotNull WidgetType<T> type,
               @NotNull Handler<T> handler, @NotNull T initialValue) {
            super(id, name, localizeName, type, handler);
            this.value = initialValue;
        }

        @Override
        public @NotNull T getValue() {
            return value;
        }

        @Override
        public @UnknownNullability T getValue(@NotNull Player player) {
            return value;
        }

        @Override
        public void setValue(@NotNull T value, boolean sync) {
            this.value = value;

            if (!sync) return;
            //todo how to sync
        }

        @Override
        public void setValue(@NotNull Player player, @NotNull T value, boolean sync) {
            this.value = value;

            if (!sync) return;
            //todo how to sync
        }
    }
}
