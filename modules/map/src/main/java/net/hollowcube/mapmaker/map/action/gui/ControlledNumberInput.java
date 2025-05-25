package net.hollowcube.mapmaker.map.action.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class ControlledNumberInput extends Panel {
    private final String key;
    private final Consumer<Integer> onChange;

    private final Text labelText;
    private final Text inputText;
    private final Button minusButton;
    private final Button plusButton;

    private Int2ObjectFunction<String> formatter = String::valueOf;
    private IntFunction<String> toString = String::valueOf;
    private ToIntFunction<String> fromString = (s) -> Integer.parseInt(s, 10);
    private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    private int smallStep = 1, bigStep = 5;

    private int value = 0;

    public ControlledNumberInput(@NotNull String key, @NotNull Consumer<Integer> onChange) {
        this(key, onChange, false);
    }

    public ControlledNumberInput(@NotNull String key, @NotNull Consumer<Integer> onChange, boolean oneSlotHack) {
        super(7, oneSlotHack ? 1 : 2);
        this.key = key;
        this.onChange = onChange;

        if (oneSlotHack) {
            this.labelText = add(0, 0, new Text(null, 7, 0,
                    LanguageProviderV2.translateToPlain("gui.action." + key))
                    .font("small").align(1, -11));
        } else {
            this.labelText = add(0, 0, AbstractActionEditorPanel.groupText(7,
                    LanguageProviderV2.translateToPlain("gui.action." + key)));
            this.labelText.translationKey("gui.action." + key);
        }

        this.inputText = add(0, oneSlotHack ? 0 : 1, new Text("gui.action." + key, 5, 1, "")
                .align(6, 5).background("generic2/input/5_1_shadow"));
        this.inputText
                .lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKEDIT)
                .onLeftClick(this::beginAnvilEdit);

        this.minusButton = add(5, oneSlotHack ? 0 : 1, new Button("gui.action." + key + ".minus", 1, 1)
                .background("generic2/btn/default/1_1_shadow")
                .sprite("generic2/icon/minus", 4, 8)
                .onLeftClick(() -> handleNewValue(value - smallStep))
                .onShiftLeftClick(() -> handleNewValue(value - bigStep)));
        this.plusButton = add(6, oneSlotHack ? 0 : 1, new Button("gui.action." + key + ".plus", 1, 1)
                .background("generic2/btn/default/1_1_shadow")
                .sprite("generic2/icon/plus", 4, 4)
                .onLeftClick(() -> handleNewValue(value + smallStep))
                .onShiftLeftClick(() -> handleNewValue(value + bigStep)));
    }

    public @NotNull ControlledNumberInput label(@NotNull String text) {
        var translationKey = "gui.action." + key + "." + text;
        this.inputText.translationKey(translationKey);
        this.labelText.translationKey(translationKey);
        this.labelText.text(LanguageProviderV2.translateToPlain(translationKey));
        return this;
    }

    public @NotNull ControlledNumberInput formatted(@NotNull Int2ObjectFunction<String> formatter) {
        this.formatter = formatter;
        return this;
    }

    public @NotNull ControlledNumberInput parsed(@NotNull IntFunction<String> toString, @NotNull ToIntFunction<String> fromString) {
        this.toString = toString;
        this.fromString = fromString;
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

    private void beginAnvilEdit() {
        host.pushView(simpleAnvil(
                "generic2/anvil/field_container",
                "action/anvil/" + this.key.replace(".", "_") + "_icon",
                LanguageProviderV2.translateToPlain("gui.action." + key),
                this::receiveAnvilEdit, this.toString.apply(this.value)
        ));
    }

    private void receiveAnvilEdit(@NotNull String newValue) {
        try {
            handleNewValue(this.fromString.applyAsInt(newValue));
        } catch (NumberFormatException ignored) {
            // Don't need to do anything
        }
    }

    private void handleNewValue(int newValue) {
        newValue = Math.clamp(newValue, this.min, this.max);
        if (this.value == newValue) return;

        this.onChange.accept(newValue);
    }
}
