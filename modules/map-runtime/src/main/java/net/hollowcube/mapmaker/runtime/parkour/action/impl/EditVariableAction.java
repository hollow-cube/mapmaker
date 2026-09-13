package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.MolangExpression;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.variables.VariableEditor;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.variables.VariableQueries;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.variables.VariableStorage;
import net.hollowcube.molang.MolangState;
import net.hollowcube.molang.runtime.ContentError;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record EditVariableAction(
        @Nullable String variable,
        MolangExpression<VariableQueries.Context> expression
) implements Action {

    public static final Key KEY = Key.key("mapmaker:variable");
    public static final StructCodec<EditVariableAction> CODEC = StructCodec.struct(
            "variable", Codec.STRING.optional(), EditVariableAction::variable,
            "expression", MolangExpression.codec(VariableQueries.ENVIRONMENT).optional(MolangExpression.from(VariableQueries.ENVIRONMENT, "0")), EditVariableAction::expression,
            EditVariableAction::new
    );

    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-z_]{3,25}$");
    private static final Sprite SPRITE = new Sprite("action/icon/variable", 1, 2);

    public static final Editor<EditVariableAction> EDITOR = new Editor<>(
            VariableEditor::new, SPRITE, VariableEditor::thumbnail
    );

    public EditVariableAction withVariable(String variable) {
        return new EditVariableAction(variable, this.expression);
    }

    public EditVariableAction withExpression(String expression) {
        return new EditVariableAction(this.variable, MolangExpression.from(VariableQueries.ENVIRONMENT, expression));
    }

    public boolean isValidVariableName() {
        return this.variable != null && VARIABLE_NAME_PATTERN.matcher(this.variable).matches();
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        if (this.variable == null) return;
        if (this.expression.error() != null) return;
        if (this.expression.program() == null) return;
        if (!isValidVariableName()) return;

        List<ContentError> errors;

        try {
            var variables = Objects.requireNonNullElseGet(state.get(Attachments.VARIABLES), VariableStorage::new);
            var molangState = new MolangState();
            var value = this.expression.program().eval(molangState, new VariableQueries.Context(player, variables));
            state.set(Attachments.VARIABLES, variables.with(this.variable, value));
            errors = molangState.getErrors();
        } catch (Exception exception) {
            // Sanity check for unexpected errors, but molang should handle errors gracefully
            ExceptionReporter.reportException(exception, player);
            errors = List.of(new ContentError("Internal Server Error, please report to administrators if persistent."));
        }

        var world = MapWorld.forPlayer(player);
        if (world != null && !world.map().isPublished() && !errors.isEmpty()) {
            var error = errors.stream().map(ContentError::toString).collect(Collectors.joining("\n"));
            player.sendMessage(Component.text("Errors setting variable '" + this.variable + "':\n" + error));
        }
    }
}
