package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiSelect<T> extends Panel {
    private final List<Runnable> onChange = new ArrayList<>();

    private final List<T> items = new ArrayList<>();
    private int count = 0;

    public MultiSelect(int slotWidth) {
        super(slotWidth, 1);
    }

    public @NotNull MultiSelect<T> onChange(@NotNull Runnable onChange) {
        this.onChange.add(onChange);
        return this;
    }

    public @NotNull List<T> selectedItems() {
        return items;
    }

    public void addOption(@NotNull T t, @NotNull String translationKey, @NotNull String icon, int iconX, int iconY) {
        var button = add(count++, 0, new Button(null, 1, 1));
        Runnable update = () -> {
            if (!items.contains(t)) {
                button.translationKey(translationKey + ".off");
                button.background("generic2/btn/default/1_1ex");
                button.sprite(icon, iconX, iconY);
            } else {
                button.translationKey(translationKey + ".on");
                button.background("generic2/btn/selected/1_1ex");
                button.sprite(icon, iconX, iconY + 2);
            }
        };
        button.onLeftClick(() -> {
            if (items.contains(t)) items.remove(t);
            else items.add(t);
            update.run();
            onChange.forEach(Runnable::run);
        });
        update.run();
    }
}
