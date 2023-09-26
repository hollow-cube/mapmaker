package net.hollowcube.terraform.compat.axiom.world.property;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface WidgetType<T> permits
        WidgetTypes.CheckboxType,
        WidgetTypes.SliderType,
        WidgetTypes.TextBoxType,
        WidgetTypes.TimeType,
        WidgetTypes.ButtonType,
        WidgetTypes.ButtonArrayType {

    static @NotNull WidgetType<Boolean> Checkbox() {
        return WidgetTypes.CheckboxType.INSTANCE;
    }

    static @NotNull WidgetType<Integer> Slider(int min, int max) {
        return new WidgetTypes.SliderType(min, max);
    }

    static @NotNull WidgetType<String> TextBox() {
        return WidgetTypes.TextBoxType.INSTANCE;
    }

    static @NotNull WidgetType<Integer> Time() {
        return WidgetTypes.TimeType.INSTANCE;
    }

    static @NotNull WidgetType<Void> Button() {
        return WidgetTypes.ButtonType.INSTANCE;
    }

    static @NotNull WidgetType<Integer> ButtonArray(@NotNull String... buttons) {
        return new WidgetTypes.ButtonArrayType(List.of(buttons));
    }

    static @NotNull WidgetType<Integer> ButtonArray(@NotNull List<String> buttons) {
        return new WidgetTypes.ButtonArrayType(buttons);
    }

    int typeId();

    @NotNull DataType<T> dataType();

    byte[] properties();

}
