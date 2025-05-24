package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.panels.AbstractAnvilView;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ActionEditorAnvil<T extends Action> extends AbstractAnvilView {
    private final ActionList.Ref ref;
    private final BiFunction<T, String, T> fromString;

    public ActionEditorAnvil(@NotNull ActionList.Ref ref, @NotNull Function<T, String> toString,
                             @NotNull BiFunction<T, String, T> fromString) {
        super("generic2/anvil/field_container", "action/anvil/" +
                        ref.key().value().replace(".", "_") + "_icon",
                toString.apply(ref.cast()));
        this.ref = ref;
        this.fromString = fromString;
    }

    @Override
    protected void onSubmit(@NotNull String text) {
        try {
            ref.<T>update(data -> fromString.apply(data, text));
        } catch (Exception e) {
            ExceptionReporter.reportException(e, host.player());
        }
        super.onSubmit(text); // Pop the view
    }

}
