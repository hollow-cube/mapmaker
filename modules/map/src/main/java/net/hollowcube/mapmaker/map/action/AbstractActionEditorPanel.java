package net.hollowcube.mapmaker.map.action;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.kyori.adventure.text.Component.translatable;

public abstract class AbstractActionEditorPanel<T> extends Panel {
    private final AbstractAction<T> action;

    protected final Text subtitleText;

    protected AbstractActionEditorPanel(@NotNull AbstractAction<T> action) {
        super(9, 10);
        this.action = action;

        background("action/editor/container");
        add(0, 0, title(LanguageProviderV2.translateToPlain(translatable(translationKey("title")))));

        add(0, 0, backOrClose());
        add(1, 0, infoWithKey(translationKey("info")));
        this.subtitleText = add(2, 0, new Text(null, 5, 1, "")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("todo", 2, 1)
                .background("generic2/btn/default/2_1"));
    }

    private @NotNull String translationKey(@NotNull String key) {
        return "gui." + action.key().namespace() + ".action." + action.key().value() + "." + key;
    }
}
