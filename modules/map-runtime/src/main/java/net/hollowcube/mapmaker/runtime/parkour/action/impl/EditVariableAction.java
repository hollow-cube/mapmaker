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
import net.hollowcube.mapmaker.runtime.parkour.action.util.MolangResolver;
import net.hollowcube.molang.eval.MolangEvaluator;
import net.hollowcube.molang.runtime.ContentError;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record EditVariableAction(
        @Nullable String variable,
        MolangExpression expression
) implements Action {

    public static final Key KEY = Key.key("mapmaker:variable");
    public static final StructCodec<EditVariableAction> CODEC = StructCodec.struct(
            "variable", Codec.STRING.optional(), EditVariableAction::variable,
            "expression", MolangExpression.CODEC.optional(MolangExpression.ZERO), EditVariableAction::expression,
            EditVariableAction::new
    );

    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-z_]{3,25}$");
    private static final Sprite SPRITE = new Sprite("action/icon/variable", 1, 2);

    private static final VariableStorage.MolangLookup VARIABLE_LOOKUP = VariableStorage.lookup();
    private static final MolangResolver<Player> QUERY = new MolangResolver<>(VariableQueries::resolve);
    private static final MolangEvaluator EVALUATOR = new MolangEvaluator(Map.of(
            "variable", VARIABLE_LOOKUP,
            "v", VARIABLE_LOOKUP,
            "query", QUERY,
            "q", QUERY
    ));

    public static final Editor<EditVariableAction> EDITOR = new Editor<>(
            VariableEditor::new, SPRITE, VariableEditor::thumbnail
    );

    public EditVariableAction withVariable(String variable) {
        return new EditVariableAction(variable, this.expression);
    }

    public EditVariableAction withExpression(String expression) {
        return new EditVariableAction(this.variable, MolangExpression.from(expression));
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
        if (this.expression.parsed() == null) return;
        if (!isValidVariableName()) return;

        List<ContentError> errors;

        try {
            var variables = Objects.requireNonNullElseGet(state.get(Attachments.VARIABLES), VariableStorage::new);
            VARIABLE_LOOKUP.setStorage(variables);
            QUERY.setContext(player);
            state.set(Attachments.VARIABLES, variables.with(this.variable, EVALUATOR.eval(this.expression.parsed())));
            errors = EVALUATOR.getErrors();
        } catch (ArithmeticException exception) {
            errors = List.of(new ContentError(exception.getMessage()));
        } catch (Exception exception) {
            // Sanity check for unexpected errors, but molang should handle errors gracefully
            ExceptionReporter.reportException(exception, player);
            errors = List.of(new ContentError("Internal Server Error, please report to administrators if persistent."));
        }

        var world = MapWorld.forPlayer(player);
        if (world != null && !world.map().isPublished() && !errors.isEmpty()) {
            var error = errors.stream().map(ContentError::message).collect(Collectors.joining("\n"));
            player.sendMessage(Component.text("Errors setting variable '" + this.variable + "':\n" + error));
        }
    }
}
