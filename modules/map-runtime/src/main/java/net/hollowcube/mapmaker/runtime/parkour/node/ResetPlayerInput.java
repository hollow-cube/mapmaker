package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.mapmaker.runtime.io.Dispatch;
import net.hollowcube.mapmaker.runtime.io.PlayerInput;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.CheckpointResetEvent;
import net.kyori.adventure.key.Key;

import java.util.Objects;

/// @param hard starts a new run instead of returning to the last checkpoint
public record ResetPlayerInput(boolean hard) implements PlayerInput {
    public static final Key KEY = Key.key("mapmaker", "reset");

    @Override
    public void deliver(Dispatch.Delivery delivery) {
        var player = delivery.player();
        var world = Objects.requireNonNull(ParkourMapWorld.forPlayer(player));
        if (hard) world.hardResetPlayer(player);
        else world.softResetPlayer(player, CheckpointResetEvent.Reason.FORCED);
    }

    @Override
    public boolean replacesState() {
        return true;
    }
}
