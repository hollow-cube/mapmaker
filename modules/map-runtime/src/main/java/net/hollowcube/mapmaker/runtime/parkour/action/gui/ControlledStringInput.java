package net.hollowcube.mapmaker.runtime.parkour.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;

import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.LORE_POSTFIX_CLICKEDIT;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class ControlledStringInput extends Panel {
    private final String key;
    private final Consumer<String> onChange;

    private final Text labelText;
    private final Text inputText;

    private String value = "";

    public ControlledStringInput(String key, Consumer<String> onChange) {
        this(key, onChange, false);
    }

    public ControlledStringInput(String key, Consumer<String> onChange, boolean oneSlotHack) {
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

        this.inputText = add(0, oneSlotHack ? 0 : 1, new Text("gui.action." + key, 7, 1, "")
            .align(6, 5).background("generic2/input/7_1_shadow"));
        this.inputText
            .lorePostfix(LORE_POSTFIX_CLICKEDIT)
            .onLeftClick(this::beginAnvilEdit);
    }

    public ControlledStringInput label(String text) {
        var translationKey = "gui.action." + key + "." + text;
        this.inputText.translationKey(translationKey);
        this.labelText.translationKey(translationKey);
        this.labelText.text(LanguageProviderV2.translateToPlain(translationKey));
        return this;
    }

    public void update(String value) {
        this.value = value;
        this.inputText.text(value);
    }

    private void beginAnvilEdit() {
        host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            "action/anvil/" + this.key.replace(".", "_") + "_icon",
            LanguageProviderV2.translateToPlain("gui.action." + key + ".name"),
            this::receiveAnvilEdit, this.value
        ));
    }

    private void receiveAnvilEdit(String newValue) {
        if (newValue.equals(this.value)) return;
        this.onChange.accept(newValue);
    }
}
