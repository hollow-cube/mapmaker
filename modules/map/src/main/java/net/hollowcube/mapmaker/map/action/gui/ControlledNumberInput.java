package net.hollowcube.mapmaker.map.action.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ControlledNumberInput extends Panel {
    private final Consumer<Integer> onChange;

    private final Text labelText;
    private final Text inputText;
    private final Button minusButton;
    private final Button plusButton;

    private Int2ObjectFunction<String> formatter = String::valueOf;
    private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    private int smallStep = 1, bigStep = 5;

    private int value = 0;

    public ControlledNumberInput(@NotNull Consumer<Integer> onChange) {
        super(7, 2);
        this.onChange = onChange;

        this.labelText = add(0, 0, new Text("abc", 7, 1, "")
                .font("small").align(1, 6));

        this.inputText = add(0, 1, new Text("aaa", 5, 1, "")
                .align(6, 5)
                .background("generic2/input/5_1_shadow"));
        this.minusButton = add(5, 1, new Button("aaa", 1, 1)
                .background("generic2/btn/default/1_1_shadow")
                .sprite("generic2/icon/minus", 4, 8)
                .onLeftClick(() -> handleNewValue(value - smallStep))
                .onShiftLeftClick(() -> handleNewValue(value - bigStep)));
        this.plusButton = add(6, 1, new Button("aaa", 1, 1)
                .background("generic2/btn/default/1_1_shadow")
                .sprite("generic2/icon/plus", 4, 4)
                .onLeftClick(() -> handleNewValue(value + smallStep))
                .onShiftLeftClick(() -> handleNewValue(value + bigStep)));
    }

    public @NotNull ControlledNumberInput label(@NotNull String text) {
        this.labelText.text(text);
        return this;
    }

    public @NotNull ControlledNumberInput formatted(@NotNull Int2ObjectFunction<String> formatter) {
        this.formatter = formatter;
        return this;
    }

    public @NotNull ControlledNumberInput range(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public @NotNull ControlledNumberInput stepped(int small, int big) {
        this.smallStep = small;
        this.bigStep = big;
        return this;
    }

    public void update(int value) {
        this.value = Math.clamp(value, this.min, this.max);
        this.inputText.text(this.formatter.apply(this.value));

        if (this.value == this.min) minusButton.background("generic2/btn/disabled/1_1_shadow");
        else minusButton.background("generic2/btn/default/1_1_shadow");
        if (this.value == this.max) plusButton.background("generic2/btn/disabled/1_1_shadow");
        else plusButton.background("generic2/btn/default/1_1_shadow");
    }

    private void handleNewValue(int newValue) {
        newValue = Math.clamp(newValue, this.min, this.max);
        if (this.value == newValue) return;

        this.onChange.accept(newValue);
    }
}
