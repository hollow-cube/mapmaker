package net.hollowcube.mapmaker.map.action;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.kyori.adventure.text.Component.translatable;

public abstract class AbstractActionEditorPanel<T> extends Panel {
    protected final ActionList.ActionData<T> actionData;
    public boolean isTransient = false; // todo handle this better

    protected final Text subtitleText;

    protected AbstractActionEditorPanel(@NotNull ActionList.ActionData<T> actionData) {
        super(9, 10);
        this.actionData = actionData;

        background("action/editor/container", -10, -31);
        add(0, 0, title(LanguageProviderV2.translateToPlain(translatable(translationKey("title")))));

        add(0, 0, backOrClose());
        add(1, 0, infoWithKey(translationKey("info")));
        this.subtitleText = add(2, 0, new Text(null, 5, 1, "")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("todo", 2, 1)
                .background("generic2/btn/default/2_1"));
    }

    protected abstract void update(@NotNull T data);

    protected <V> @NotNull Consumer<V> update(@NotNull BiFunction<T, V, T> updater) {
        return value -> {
            try {
                var newData = updater.apply(actionData.getData(), value);
                actionData.setData(newData);
                update(newData);
            } catch (Exception e) {
                ExceptionReporter.reportException(e, host.player());
            }
        };
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        Check.notNull(this.actionData, "action data was not mounted");

        update(actionData.getData());
    }

    protected @NotNull String translationKey(@NotNull String key) {
        return "gui.action." + actionData.action().key().value() + "." + key;
    }

    protected @NotNull String translate(@NotNull String key) {
        return LanguageProviderV2.translateToPlain(translatable(translationKey(key)));
    }

    public static @NotNull Text groupText(int width, @NotNull String text) {
        return new Text("todo", width, 1, text)
                .font("small").align(1, 6);
    }
}
