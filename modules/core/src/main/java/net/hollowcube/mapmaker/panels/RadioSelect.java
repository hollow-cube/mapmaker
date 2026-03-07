package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class RadioSelect<T extends @UnknownNullability Object> extends Panel {

    private final List<Runnable> buttonUpdaters = new ArrayList<>();
    private final List<Consumer<T>> onChange = new ArrayList<>();
    private final Set<T> options = new HashSet<>();

    private T selected;
    // TODO: should not be public, need to fix the cursed stuff in NewMapView
    public int index = 0;

    public RadioSelect(int slotWidth, int slotHeight) {
        this(slotWidth, slotHeight, null);
    }

    public RadioSelect(int slotWidth, int slotHeight, T defaultValue) {
        super(slotWidth, slotHeight);
        this.selected = defaultValue;
    }

    public T selected() {
        return this.selected;
    }

    public RadioSelect<T> onChange(Consumer<T> onChange) {
        this.onChange.add(onChange);
        return this;
    }

    public Button addOption(T item, ButtonUpdater updater) {
        return this.addOption(item, updater, Button::new);
    }

    public Button addOption(T item, ButtonUpdater updater, Button.Constructor buttonCtor) {
        this.options.add(item);

        int x = this.index % this.slotWidth;
        int y = this.index / this.slotWidth;

        var button = add(x, y, buttonCtor.construct(null, 1, 1));
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

    public void setSelected(T item) {
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

        ButtonUpdater SQUARE_BACKGROUND_EX = (button, selected) -> {
            var key = selected ? "generic2/btn/selected/1_1ex" : "generic2/btn/default/1_1ex";
            button.background(key);

            final var sprite = button.sprite;
            if (sprite != null) {
                // This assumes that all icons are 16x16, which is fine for now.
                button.sprite(sprite.withOffset(sprite.offsetX(), selected ? 3 : 1));
            }
        };

        void update(Button button, boolean selected);
    }
}
