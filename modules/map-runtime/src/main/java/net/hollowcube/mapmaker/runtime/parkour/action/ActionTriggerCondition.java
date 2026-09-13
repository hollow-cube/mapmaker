package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.runtime.parkour.action.impl.variables.VariableQueries;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

public record ActionTriggerCondition(
        @Nullable MolangExpression<VariableQueries.Context> expression,
        boolean showMessage,
        String message
) {

    public static final ActionTriggerCondition DEFAULT = new ActionTriggerCondition(null, true, "");
    public static final Codec<ActionTriggerCondition> CODEC = StructCodec.struct(
            "expression", MolangExpression.codec(VariableQueries.ENVIRONMENT).optional(), ActionTriggerCondition::expression,
            "show_message", Codec.BOOLEAN.optional(true), ActionTriggerCondition::showMessage,
            "message", Codec.STRING.optional(""), ActionTriggerCondition::message,
            ActionTriggerCondition::new
    );

    public ActionTriggerCondition withCondition(@Nullable MolangExpression<VariableQueries.Context> condition) {
        return new ActionTriggerCondition(condition, this.showMessage, this.message);
    }

    public ActionTriggerCondition withShowMessage(boolean showMessage) {
        return new ActionTriggerCondition(this.expression, showMessage, this.message);
    }

    public ActionTriggerCondition withMessage(String message) {
        return new ActionTriggerCondition(this.expression, this.showMessage, message);
    }
}
