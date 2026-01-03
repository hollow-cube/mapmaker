package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Select<T extends @UnknownNullability Object> extends Panel {
    private final List<Runnable> onChange = new ArrayList<>();

    private T value;
    private int count = 0;

    public Select(int slotWidth, T defaultValue) {
        super(slotWidth, 1);
        value = defaultValue;
    }

    public @NotNull Select<T> onChange(@NotNull Runnable onChange) {
        this.onChange.add(onChange);
        return this;
    }

    public T selected() {
        return value;
    }

    public void addOption(@NotNull T t, @NotNull String translationKey, @NotNull String icon, int iconX, int iconY) {
        var button = add(count++, 0, new Button(null, 1, 1));
        Runnable update = () -> {
            if (!Objects.equals(value, t)) {
                button.translationKey(translationKey + ".off");
                button.background("generic2/btn/default/1_1ex");
                button.sprite(icon, iconX, iconY);
            } else {
                button.translationKey(translationKey + ".on");
                button.background("generic2/btn/selected/1_1ex");
                button.sprite(icon, iconX, iconY + 2);
            }
        };
        onChange(update);
        button.onLeftClick(() -> {
            value = t;
            onChange.forEach(Runnable::run);
        });
        update.run();
    }
}
