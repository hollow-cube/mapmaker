package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class ControlledDecimalInput extends Panel {

    private static final DecimalFormat DEFAULT_FORMATTER = new DecimalFormat("0.0#####");

    private final String key;
    private final Consumer<Double> onChange;

    private final Text labelText;
    private final Text inputText;
    private final Button minusButton;
    private final Button plusButton;

    private DoubleFunction<String> formatter = DEFAULT_FORMATTER::format;
    private DoubleFunction<String> toString = DEFAULT_FORMATTER::format;
    private ToDoubleFunction<String> fromString = Double::parseDouble;
    private double min = Double.MIN_VALUE, max = Double.MAX_VALUE;
    private double smallStep = 0.1, bigStep = 1;

    private double value = 0;

    public ControlledDecimalInput(@NotNull String key, @NotNull Consumer<Double> onChange) {
        this(key, onChange, false);
    }

    public ControlledDecimalInput(@NotNull String key, @NotNull Consumer<Double> onChange, boolean oneSlotHack) {
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

    public @NotNull ControlledDecimalInput label(@NotNull String text) {
        var translationKey = "gui.action." + key + "." + text;
        this.inputText.translationKey(translationKey);
        this.labelText.translationKey(translationKey);
        this.labelText.text(LanguageProviderV2.translateToPlain(translationKey));
        return this;
    }

    public @NotNull ControlledDecimalInput formatted(@NotNull DoubleFunction<String> formatter) {
        this.formatter = formatter;
        return this;
    }

    public @NotNull ControlledDecimalInput parsed(@NotNull DoubleFunction<String> toString, @NotNull ToDoubleFunction<String> fromString) {
        this.toString = toString;
        this.fromString = fromString;
        return this;
    }

    public @NotNull ControlledDecimalInput range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public @NotNull ControlledDecimalInput stepped(double small, double big) {
        this.smallStep = small;
        this.bigStep = big;
        return this;
    }

    public void update(double value) {
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
                LanguageProviderV2.translateToPlain("gui.action." + key + ".name"),
                this::receiveAnvilEdit, this.toString.apply(this.value)
        ));
    }

    private void receiveAnvilEdit(@NotNull String newValue) {
        try {
            handleNewValue(this.fromString.applyAsDouble(newValue));
        } catch (NumberFormatException ignored) {
            // Don't need to do anything
        }
    }

    private void handleNewValue(double newValue) {
        newValue = Math.clamp(newValue, this.min, this.max);
        if (this.value == newValue) return;

        this.onChange.accept(newValue);
    }
}
