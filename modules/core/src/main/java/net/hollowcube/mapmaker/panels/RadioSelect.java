package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class RadioSelect<T> extends Panel {

    private final List<Runnable> buttonUpdaters = new ArrayList<>();
    private final List<Consumer<@Nullable T>> onChange = new ArrayList<>();
    private final Set<T> options = new HashSet<>();

    private T selected = null;
    private int index = 0;

    public RadioSelect(int slotWidth, int slotHeight) {
        super(slotWidth, slotHeight);
    }

    public @Nullable T selected() {
        return this.selected;
    }

    public @NotNull RadioSelect<T> onChange(@NotNull Consumer<@NotNull T> onChange) {
        this.onChange.add(onChange);
        return this;
    }

    public Button addOption(@NotNull T item, @NotNull ButtonUpdater updater) {
        this.options.add(item);

        int x = this.index % this.slotWidth;
        int y = this.index / this.slotWidth;

        var button = add(x, y, new Button(null, 1, 1));
        Runnable update = () -> updater.update(button, item.equals(this.selected));
        this.buttonUpdaters.add(update);
        button.onLeftClick(() -> {
            this.selected = item;
            this.buttonUpdaters.forEach(Runnable::run);
            this.onChange.forEach(consumer -> consumer.accept(item));
        });
        update.run();

        this.index++;

        return button;
    }

    public void setSelected(@Nullable T item) {
        if (item == null || this.options.contains(item)) {
            this.selected = item;
            this.buttonUpdaters.forEach(Runnable::run);
            if (item != null) {
                this.onChange.forEach(consumer -> consumer.accept(item));
            }
        }
    }

    @FunctionalInterface
    public interface ButtonUpdater {

        ButtonUpdater SQUARE_BACKGROUND = (button, selected) -> {
            var key = selected ? "generic2/btn/selected/1_1" : "generic2/btn/default/1_1";
            button.background(key);
        };

        void update(@NotNull Button button, boolean selected);
    }
}
