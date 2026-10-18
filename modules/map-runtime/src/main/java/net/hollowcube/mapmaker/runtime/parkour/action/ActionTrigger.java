package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;

public record ActionTrigger(Kind kind, String id, Point position, ActionTriggerData data) implements Trigger {

    public enum Kind {
        CHECKPOINT,
        STATUS,
    }

    @Override
    public void onEnter(MapWorld world, Player player) {
        switch (kind) {
            case CHECKPOINT -> TempEffectApplicator.applyCheckpoint(data, player, id, position);
            case STATUS -> TempEffectApplicator.applyStatus(data, player, id);
        }
    }

    @Override
    public void onExit(MapWorld world, Player player) {
        if (kind == Kind.CHECKPOINT)
            TempEffectApplicator.handleCheckpointExit(player, data, id);
    }

}
