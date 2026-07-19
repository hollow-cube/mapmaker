package net.hollowcube.mapmaker.runtime.parkour.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.panels.AbstractAnvilView;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ActionEditorAnvil<T extends Action> extends AbstractAnvilView {
    private final ActionList.Ref ref;
    private final BiFunction<T, String, T> fromString;

    public ActionEditorAnvil(
        ActionList.Ref ref,
        Function<T, String> toString,
        BiFunction<T, String, T> fromString
    ) {
        super("generic2/anvil/field_container", "action/anvil/" + ref.key().value().replace(".", "_") + "_icon",
            LanguageProviderV2.translateToPlain("gui.action." + ref.key().value().replace(".", "_") + ".title"),
            toString.apply(ref.cast()), true);
        this.ref = ref;
        this.fromString = fromString;
    }

    protected T parse(T data, String text) {
        return this.fromString.apply(data, text);
    }

    protected boolean validateResult(T result) {
        return true;
    }

    @Override
    protected void onSubmit(String text) {
        final var submitHost = this.host;
        if (submitHost == null) return;

        try {
            ref.<T>update(data -> {
                var newValue = this.parse(data, text);
                // Parsing may intentionally close the inventory to report invalid input.
                // Do not validate or commit after that synchronously unmounts this editor.
                if (this.host != submitHost) return data;
                if (!validateResult(newValue)) return data;
                return newValue;
            });
        } catch (Exception e) {
            ExceptionReporter.reportException(e, submitHost.player());
        }
        super.onSubmit(text); // Pop the view
    }

}
