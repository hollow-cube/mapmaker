package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.common.util.OpUtils;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

// Note that while both checkpoints and status plates use this struct, repeatable is not currently supported on checkpoints.
public record ActionTriggerData(
        ActionList actions,
        boolean repeatable,
        ActionTriggerCondition condition
) {
    public static final ActionTriggerData EMPTY = new ActionTriggerData(new ActionList(), false, ActionTriggerCondition.DEFAULT);

    private static final int MAX_ACTIONS = 21;
    public static final StructCodec<ActionTriggerData> CODEC = StructCodec.struct(
            "actions", ActionRegistry.listCodec(MAX_ACTIONS), ActionTriggerData::actions,
            "repeatable", Codec.BOOLEAN.optional(false), ActionTriggerData::repeatable,
            "condition", ActionTriggerCondition.CODEC.optional(ActionTriggerCondition.DEFAULT), ActionTriggerData::condition,
            ActionTriggerData::new
    );

    public ActionTriggerData {
        actions = new ActionList(actions);
    }

    public ActionTriggerData() {
        this(new ActionList(), false, ActionTriggerCondition.DEFAULT);
    }

    public Mutable toMutable() {
        return new Mutable(actions, repeatable, condition);
    }

    public static class Mutable {

        private final ActionList actions;
        private boolean repeatable;
        private ActionTriggerCondition condition;

        private Mutable(ActionList actions, boolean repeatable, ActionTriggerCondition condition) {
            this.actions = new ActionList(actions);
            this.repeatable = repeatable;
            this.condition = condition;
        }

        public void setRepeatable(boolean repeatable) {
            this.repeatable = repeatable;
        }

        public void setConditionExpression(@Nullable String condition) {
            this.condition = this.condition.withCondition(OpUtils.map(condition, MolangExpression::from));
        }

        public void setConditionMessage(String message) {
            this.condition = this.condition.withMessage(message);
        }

        public void setShowConditionMessage(boolean show) {
            this.condition = this.condition.withShowMessage(show);
        }

        public ActionList actions() {
            return actions;
        }

        public boolean isRepeatable() {
            return repeatable;
        }

        public ActionTriggerCondition condition() {
            return condition;
        }

        public ActionTriggerData toImmutable() {
            return new ActionTriggerData(actions, repeatable, condition);
        }
    }
}
