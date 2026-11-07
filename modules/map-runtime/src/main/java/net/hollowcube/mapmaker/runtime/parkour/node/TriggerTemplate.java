package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.mapmaker.runtime.io.Connection;
import net.hollowcube.mapmaker.runtime.io.PlayerInput;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;

import java.util.List;
import java.util.Map;

public final class TriggerTemplate {

    public static TriggerNodeData empty() {
        return TriggerNodeData.EMPTY;
    }

    public static TriggerNodeData checkpoint() {
        return new TriggerNodeData(
            false,
            ActionTriggerCondition.DEFAULT,
            new LegacyRules(true, false, true),
            List.of(enter(new CheckpointPlayerInput(null, false)))
        );
    }

    /// spec checkpoint
    public static TriggerNodeData temporaryCheckpoint() {
        var checkpoint = checkpoint();
        return new TriggerNodeData(
            false,
            checkpoint.condition(),
            checkpoint.legacy(),
            List.of(enter(new CheckpointPlayerInput(null, true)))
        );
    }

    public static TriggerNodeData status() {
        return new TriggerNodeData(
            true,
            ActionTriggerCondition.DEFAULT,
            new LegacyRules(false, true, false),
            List.of()
        );
    }

    public static TriggerNodeData finish() {
        return new TriggerNodeData(
            false,
            ActionTriggerCondition.DEFAULT,
            LegacyRules.NONE,
            List.of(enter(new FinishPlayerInput()))
        );
    }

    public static TriggerNodeData reset(boolean hard) {
        return new TriggerNodeData(
            false,
            ActionTriggerCondition.DEFAULT,
            LegacyRules.NONE,
            List.of(enter(new ResetPlayerInput(hard)))
        );
    }

    public static final Map<String, TriggerNodeData> PRESETS = Map.of(
        "mapmaker:checkpoint", TriggerTemplate.checkpoint(),
        "mapmaker:status", TriggerTemplate.status(),
        "mapmaker:finish", TriggerTemplate.finish(),
        "mapmaker:reset", TriggerTemplate.reset(false)
    );

    private static Connection enter(PlayerInput input) {
        return new Connection(TriggerNode.ENTER, TriggerNodeData.inputKey(input), input);
    }

    private TriggerTemplate() {}
}
