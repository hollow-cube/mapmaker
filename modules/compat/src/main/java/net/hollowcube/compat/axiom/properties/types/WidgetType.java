package net.hollowcube.compat.axiom.properties.types;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public record WidgetType<T>(
        int id,
        DataType<T> type,
        Consumer<NetworkBuffer> extraData
) {

    public static WidgetType<Boolean> Checkbox() {
        return new WidgetType<>(0, DataType.BOOLEAN, buffer -> {});
    }

    public static WidgetType<Integer> Slider(int min, int max) {
        return new WidgetType<>(1, DataType.INTEGER, buffer -> {
            buffer.write(NetworkBuffer.INT, min);
            buffer.write(NetworkBuffer.INT, max);
        });
    }

    public static WidgetType<String> Textbox() {
        return new WidgetType<>(2, DataType.STRING, buffer -> {});
    }

    public static WidgetType<Unit> Time() {
        return new WidgetType<>(3, DataType.UNIT, buffer -> {});
    }

    public static WidgetType<Unit> Button() {
        return new WidgetType<>(4, DataType.UNIT, buffer -> {});
    }

    public static WidgetType<Integer> Radio(List<String> options) {
        return new WidgetType<>(5, DataType.INTEGER, buffer -> buffer.write(NetworkBuffer.STRING.list(), options));
    }

    public void write(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, this.id);
        this.extraData.accept(buffer);
    }
}
