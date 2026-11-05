package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.runtime.io.Connection;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.RespawnPosAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.SetProgressIndexAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.minestom.server.coordinate.Point;

import java.util.ArrayList;
import java.util.List;

public final class LegacyTriggers {

    public static TriggerNode reset(String id, Point position, boolean hard) {
        var reset = new Connection(
            TriggerNode.ENTER,
            ResetPlayerInput.KEY,
            new ResetPlayerInput(hard)
        );
        return new TriggerNode(
            id,
            position,
            false,
            ActionTriggerCondition.DEFAULT,
            LegacyRules.NONE,
            List.of(reset)
        );
    }

    public static TriggerNode status(String id, Point position, ActionTriggerData data) {
        var legacy = new LegacyRules(progressIndex(data), false, true, false, null);
        return new TriggerNode(
            id,
            position,
            !data.repeatable(),
            data.condition(),
            legacy,
            data.actions().connections(TriggerNode.ENTER)
        );
    }

    public static TriggerNode checkpoint(String id, Point position, ActionTriggerData data) {
        return checkpoint(id, position, data, false);
    }

    public static TriggerNode temporaryCheckpoint(String id, Point position) {
        return checkpoint(id, position, new ActionTriggerData(), true);
    }

    public static TriggerNode finish(String id, Point position) {
        var finish = new Connection(
            TriggerNode.ENTER,
            FinishPlayerInput.KEY,
            new FinishPlayerInput()
        );
        return new TriggerNode(
            id,
            position,
            false,
            ActionTriggerCondition.DEFAULT,
            LegacyRules.NONE,
            List.of(finish)
        );
    }

    private static TriggerNode checkpoint(
        String id,
        Point position,
        ActionTriggerData data,
        boolean temporary
    ) {
        var actions = data.actions();
        var respawn = actions.findLast(RespawnPosAction.class);
        // A teleporting checkpoint already sets the view through the teleport, so leaving it doesn't.
        var legacy = new LegacyRules(
            progressIndex(data),
            true,
            false,
            !actions.has(TeleportAction.KEY),
            respawn
        );
        var connections = new ArrayList<>(actions.connections(TriggerNode.ENTER));
        var checkpoint = new CheckpointPlayerInput(respawn, temporary);
        connections.add(new Connection(TriggerNode.ENTER, CheckpointPlayerInput.KEY, checkpoint));
        return new TriggerNode(
            id,
            position,
            false,
            data.condition(),
            legacy,
            List.copyOf(connections)
        );
    }

    private static int progressIndex(ActionTriggerData data) {
        return OpUtils.mapOr(
            data.actions().findLast(SetProgressIndexAction.class),
            SetProgressIndexAction::value,
            -1
        );
    }

    private LegacyTriggers() {}
}
