package net.hollowcube.mapmaker.runtime.parkour.action;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;

// Note that while both checkpoints and status plates use this struct, repeatable is not currently supported on checkpoints.
@SuppressWarnings("UnstableApiUsage")
public record ActionTriggerData(
        ActionList actions,
        boolean repeatable
) {
    private static final int MAX_ACTIONS = 21;
    public static final StructCodec<ActionTriggerData> CODEC = StructCodec.struct(
            "actions", ActionRegistry.listCodec(MAX_ACTIONS), ActionTriggerData::actions,
            "repeatable", Codec.BOOLEAN.optional(false), ActionTriggerData::repeatable,
            ActionTriggerData::new);

    public ActionTriggerData {
        actions = new ActionList(actions);
    }

    public ActionTriggerData() {
        this(new ActionList(), false);
    }

    public ActionTriggerData withRepeatable(boolean repeatable) {
        return new ActionTriggerData(actions, repeatable);
    }
}
