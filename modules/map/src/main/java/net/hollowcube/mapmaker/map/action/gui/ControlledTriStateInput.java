package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ControlledTriStateInput<E extends Enum<E>> extends Panel {
    private static final List<Component> GUI_ACTION_CLICKSELECT = LanguageProviderV2.translateMulti("gui.action.clickselect", List.of());

    private final String key;
    private final Sprite[] sprites = new Sprite[3];
    private final Consumer<E> onChange;

    private final Text labelText;
    private final Button iconButton;
    private final Text[] buttons = new Text[3];

    private E value;

    public ControlledTriStateInput(@NotNull String key, @NotNull Class<E> enumClass, @NotNull Consumer<E> onChange) {
        super(7, 2);
        this.key = key;
        var values = enumClass.getEnumConstants();
        Check.argCondition(values.length != 3, "Enum must have exactly 3 values");
        this.onChange = onChange;

        this.labelText = add(0, 0, AbstractActionEditorPanel.groupText(7, ""));

        this.iconButton = add(0, 1, new Button("aaa", 1, 1)
                .background("generic2/btn/tristate/icon", -2, -1));
        for (int i = 0; i < 3; i++) {
            this.buttons[i] = add(1 + (i * 2), 1, new Text("aaa", 2, 1, "")
                    .align(Text.CENTER, Text.CENTER));
            final int fi = i;
            this.buttons[i].sprite("generic2/btn/tristate/default")
                    .onLeftClick(() -> this.handleNewValue(values[fi]));
        }
    }

    public @NotNull ControlledTriStateInput<E> label(@NotNull String text) {
        this.iconButton.translationKey("gui.action." + key + "." + "operation");
        this.labelText.translationKey("gui.action." + key + "." + "operation");
        this.labelText.text(text);
        return this;
    }

    public @NotNull ControlledTriStateInput<E> labels(@NotNull String text1, @NotNull String text2, @NotNull String text3) {
        this.buttons[0].text(LanguageProviderV2.translateToPlain("gui.action." + key + "." + text1 + ".label"));
        this.buttons[0].translationKey("gui.action." + key + "." + text1 + ".label");
        this.buttons[1].text(LanguageProviderV2.translateToPlain("gui.action." + key + "." + text2 + ".label"));
        this.buttons[1].translationKey("gui.action." + key + "." + text2 + ".label");
        this.buttons[2].text(LanguageProviderV2.translateToPlain("gui.action." + key + "." + text3 + ".label"));
        this.buttons[2].translationKey("gui.action." + key + "." + text3 + ".label");
        return this;
    }

    public @NotNull ControlledTriStateInput<E> sprites(@NotNull Sprite sprite1, @NotNull Sprite sprite2, @NotNull Sprite sprite3) {
        this.sprites[0] = sprite1;
        this.sprites[1] = sprite2;
        this.sprites[2] = sprite3;
        return this;
    }

    public void update(@NotNull E newValue) {
        this.value = newValue;
        var sprite = this.sprites[newValue.ordinal()];
        this.iconButton.sprite(sprite);

        for (int i = 0; i < 3; i++) {
            var button = this.buttons[i];
            button.sprite("generic2/btn/tristate/" + (i == newValue.ordinal() ? "selected" : "default"));
            button.lorePostfix(i == newValue.ordinal() ? null : GUI_ACTION_CLICKSELECT);
        }
    }

    private void handleNewValue(@NotNull E newValue) {
        if (this.value == newValue) return;

        this.onChange.accept(newValue);
    }

}
