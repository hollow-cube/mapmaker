package net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.variables;

import net.hollowcube.mapmaker.gui.notifications.NotificationManager;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledStringInput;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditVariableAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class VariableEditor extends AbstractActionEditorPanel<EditVariableAction> {

    private final ControlledStringInput variableInput;
    private final ControlledStringInput expressionInput;

    public VariableEditor(ActionList.Ref ref) {
        super(ref);

        this.variableInput = add(1, 1, new ControlledStringInput("variable.variable", update(EditVariableAction::withVariable)));
        this.expressionInput = add(1, 3, new ControlledStringInput("variable.expression", update(EditVariableAction::withExpression)));
    }

    public static TranslatableComponent thumbnail(@Nullable EditVariableAction action) {
        if (action == null || action.variable() == null) {
            return Component.translatable("gui.action.variable.thumbnail.empty");
        } else if (!action.isValidVariableName()) {
            return Component.translatable("gui.action.variable.thumbnail.invalid_name");
        } else if (action.expression().error() != null) {
            return Component.translatable("gui.action.variable.thumbnail.error", List.of(
                    Component.text(action.expression().error().getMessage())
            ));
        } else {
            return Component.translatable("gui.action.variable.thumbnail", List.of(
                    Component.text(action.variable()),
                    action.expression().display()
            ));
        }
    }

    @Override
    protected <V> Consumer<V> update(BiFunction<EditVariableAction, V, EditVariableAction> updater) {
        return value -> {
            super.update(updater).accept(value);
            // We override here as if we do the onChange in the update it breaks because it gets called twice one for mount and then for the actual change
            this.onChange();
        };
    }

    @Override
    protected void update(EditVariableAction data) {
        this.variableInput.update(Objects.requireNonNullElse(data.variable(), ""));
        this.expressionInput.update(data.expression().text());
    }

    protected void onChange() {
        if (ref.action() instanceof EditVariableAction data) {
            if (data.variable() != null && !data.isValidVariableName()) {
                NotificationManager.showNotification(
                        this.host.player(),
                        "Invalid Variable Name",
                        "Use 3-25 lowercase letters and underscores only.",
                        NamedTextColor.RED
                );
            }

            if (data.expression().error() != null) {
                NotificationManager.showNotification(
                        this.host.player(),
                        "Molang Expression Error",
                        "Invalid expression, please check your syntax.",
                        NamedTextColor.RED
                );
            }
        }
    }
}