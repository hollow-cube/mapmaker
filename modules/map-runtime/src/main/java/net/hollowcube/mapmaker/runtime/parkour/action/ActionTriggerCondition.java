package net.hollowcube.mapmaker.runtime.parkour.action;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

public record ActionTriggerCondition(
        @Nullable MolangExpression expression,
        boolean showMessage,
        String message
) {

    public static final ActionTriggerCondition DEFAULT = new ActionTriggerCondition(null, true, "");
    public static final Codec<ActionTriggerCondition> CODEC = StructCodec.struct(
            "expression", MolangExpression.CODEC.optional(), ActionTriggerCondition::expression,
            "show_message", Codec.BOOLEAN.optional(true), ActionTriggerCondition::showMessage,
            "message", Codec.STRING.optional(""), ActionTriggerCondition::message,
            ActionTriggerCondition::new
    );

    public ActionTriggerCondition withCondition(@Nullable MolangExpression condition) {
        return new ActionTriggerCondition(condition, this.showMessage, this.message);
    }

    public ActionTriggerCondition withShowMessage(boolean showMessage) {
        return new ActionTriggerCondition(this.expression, showMessage, this.message);
    }

    public ActionTriggerCondition withMessage(String message) {
        return new ActionTriggerCondition(this.expression, this.showMessage, message);
    }
}
