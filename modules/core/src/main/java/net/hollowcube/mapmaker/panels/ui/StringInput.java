package net.hollowcube.mapmaker.panels.ui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.LORE_POSTFIX_CLICKEDIT;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class StringInput extends Panel {
    private final Consumer<String> onChange;
    private final @Nullable Runnable beforeEdit;
    private final Text inputText;

    private final String translationKey;
    private String anvilIcon = "action/anvil/chat_icon";

    private String value = "";

    public StringInput(String translationKey, Consumer<String> onChange) {
        this(translationKey, onChange, null);
    }

    public StringInput(String translationKey, Consumer<String> onChange, @Nullable Runnable beforeEdit) {
        super(7, 1);
        this.onChange = onChange;
        this.beforeEdit = beforeEdit;
        this.translationKey = translationKey;

        this.inputText = add(0, 0, new Text(translationKey, 7, 1, "")
            .align(6, 5).background("generic2/input/7_1_shadow"));
        this.inputText
            .lorePostfix(LORE_POSTFIX_CLICKEDIT)
            .onLeftClick(this::beginAnvilEdit);
    }

    public StringInput anvilIcon(String icon) {
        this.anvilIcon = icon;
        return this;
    }

    public void update(String value) {
        this.value = value;
        this.inputText.text(value);
    }

    private void beginAnvilEdit() {
        if (beforeEdit != null) beforeEdit.run();
        host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            anvilIcon,
            LanguageProviderV2.translateToPlain(translationKey + ".input"),
            this::receiveAnvilEdit, this.value
        ));
    }

    private void receiveAnvilEdit(String newValue) {
        if (newValue.equals(this.value)) return;
        this.onChange.accept(newValue);
    }
}
