package net.hollowcube.mapmaker.panels.buttons;

import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CycleButton<T> extends Button {

    private final List<Entry<T>> entries = new ArrayList<>();
    private final List<Consumer<T>> onChange = new ArrayList<>();

    private T selected;

    public CycleButton(int width, int height, T initial) {
        super(width, height);
        this.selected = initial;

        this.onLeftClick(() -> {
            if (this.entries.isEmpty()) return;
            int index = -1;
            for (int i = 0; i < this.entries.size(); i++) {
                if (this.entries.get(i).value().equals(this.selected)) {
                    index = i;
                    break;
                }
            }

            this.selected = this.entries.get((index + 1) % this.entries.size()).value();
            this.updateDisplay();
            for (var consumer : this.onChange) {
                consumer.accept(selected);
            }
        });
    }

    public CycleButton<T> onChange(Consumer<T> onChange) {
        this.onChange.add(onChange);
        return this;
    }

    public CycleButton<T> addOption(T value, String translation, String icon) {
        this.entries.add(new Entry<>(value, new Sprite(icon, 1, 1), Component.translatable(translation)));
        this.updateDisplay();
        return this;
    }

    public T selected() {
        return this.selected;
    }

    private void updateDisplay() {
        var lore = new ArrayList<Component>();
        for (var entry : this.entries) {
            var text = LanguageProviderV2.translateToPlain(entry.text());
            if (entry.value().equals(this.selected)) {
                this.sprite(entry.sprite());
                lore.add(TranslatableBuilder.of("gui.generic.cycle.entry.selected").with(text).build());
            } else {
                lore.add(TranslatableBuilder.of("gui.generic.cycle.entry.unselected").with(text).build());
            }
        }
        lore.add(Component.empty());
        lore.add(Component.translatable("gui.cosmetics.tab.select"));
        this.lorePostfix(lore);
    }

    private record Entry<T>(T value, Sprite sprite, Component text) {

    }
}
