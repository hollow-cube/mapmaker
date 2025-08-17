package net.hollowcube.mapmaker.runtime.parkour.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.InventoryType;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.kyori.adventure.text.Component.translatable;

public abstract class AbstractActionEditorPanel<T extends Action> extends Panel {
    public static final List<Component> LORE_POSTFIX_CLICKSELECT = LanguageProviderV2.translateMulti("gui.action.clickselect", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKEDIT = LanguageProviderV2.translateMulti("gui.action.clickedit", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKEDITORREMOVE = LanguageProviderV2.translateMulti("gui.action.clickeditorremove", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKREMOVE = LanguageProviderV2.translateMulti("gui.action.clickremove", List.of());
    public static final List<Component> LORE_POSTFIX_NOT_AVAILABLE = LanguageProviderV2.translateMulti("gui.action.unavailable", List.of());

    protected final ActionList.Ref ref;

    protected final Text subtitleText;

    protected AbstractActionEditorPanel(ActionList.Ref ref) {
        this(ref, false);
    }

    protected AbstractActionEditorPanel(ActionList.Ref ref, boolean oneSlotLessHack) {
        super(oneSlotLessHack ? InventoryType.CHEST_5_ROW : InventoryType.CHEST_6_ROW, 9, oneSlotLessHack ? 9 : 10);
        this.ref = Objects.requireNonNull(ref, "ref");

        background("action/editor/container", -10, -31);
        add(0, 0, title(LanguageProviderV2.translateToPlain(translatable(translationKey("title")))));

        add(0, 0, backOrClose());
        add(1, 0, infoWithKey(translationKey("info")));
        this.subtitleText = add(2, 0, new Text(null, 5, 1, "")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("gui.action.remove", 2, 1)
                .background("generic2/btn/default/2_1")
                .sprite("action/icon/trash", 11, 3)
                .onLeftClick(this::removeAction));
    }

    protected abstract void update(T data);

    protected <V> Consumer<V> update(BiFunction<T, V, T> updater) {
        return value -> {
            try {
                // Update the reference we have and then update the ui to the new value.
                update(ref.<T>update(data -> updater.apply(data, value)));
            } catch (Exception e) {
                ExceptionReporter.reportException(e, host.player());
            }
        };
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        update(this.ref.<T>cast());
    }

    private void removeAction() {
        this.ref.remove();
        host.popView();
    }

    protected String translationKey(String key) {
        return "gui.action." + ref.key().value() + "." + key;
    }

    protected String translate(String key) {
        return LanguageProviderV2.translateToPlain(translatable(translationKey(key)));
    }

    public static Text groupText(int width, String text) {
        return new Text(null, width, 1, text)
                .font("small").align(1, 6);
    }
}
