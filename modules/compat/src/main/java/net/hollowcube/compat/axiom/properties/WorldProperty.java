package net.hollowcube.compat.axiom.properties;

import net.hollowcube.compat.axiom.properties.types.WidgetType;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.ApiStatus;

public final class WorldProperty<T> {

    final Key id;
    final String name;
    final boolean localized;
    final WidgetType<T> widget;
    final T initialValue;
    final PropertyDispatcher dispatcher;

    @ApiStatus.Internal
    public WorldProperty(
        Key id,
        String name, boolean localized,
        WidgetType<T> widget,
        T initialValue,
        PropertyDispatcher dispatcher
    ) {
        this.id = id;
        this.name = name;
        this.localized = localized;
        this.widget = widget;
        this.initialValue = initialValue;
        this.dispatcher = dispatcher;
    }

    public Key id() {
        return id;
    }

    public WidgetType<T> widget() {
        return widget;
    }

    @ApiStatus.Internal
    public void write(Player player, NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.KEY, this.id);
        buffer.write(NetworkBuffer.STRING, this.name);
        buffer.write(NetworkBuffer.BOOLEAN, this.localized);
        this.widget.write(buffer);
        buffer.write(NetworkBuffer.BYTE_ARRAY, NetworkBuffer.makeArray(this.widget.type().codec(), this.dispatcher.get(player, this)));
    }

    public void update(Player player, byte[] data) {
        T value = this.widget.type().codec().read(NetworkBuffer.wrap(data, 0, data.length));
        this.dispatcher.set(player, this, value);
    }

}
