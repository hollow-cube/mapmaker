package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.io.Dispatch;
import net.hollowcube.mapmaker.runtime.io.PlayerInput;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;

import java.util.Objects;

public record FinishPlayerInput() implements PlayerInput {
    public static final Key KEY = Key.key("mapmaker", "finish");
    public static final StructCodec<FinishPlayerInput> CODEC = StructCodec.struct(
        FinishPlayerInput::new
    );

    @Override
    public void deliver(Dispatch.Delivery delivery) {
        var player = delivery.player();
        var world = Objects.requireNonNull(ParkourMapWorld.forPlayer(player));

        var nextState = switch (world.getPlayerState(player)) {
            case ParkourState.Playing2(var saveState) -> {
                var finishState = saveState.copy(saveState.state(PlayState.class));
                finishState.complete(System.nanoTime() / 1_000_000);
                yield new ParkourState.Finished(finishState);
            }
            case ParkourState.Testing(var saveState, var parent) -> {
                if (parent != null) yield parent;

                saveState.complete(System.nanoTime() / 1_000_000);
                world.handleTestingModeFinish(player, saveState);
                yield null;
            }
            case null, default -> null;
        };
        if (nextState != null) world.changePlayerState(player, nextState);
    }

    @Override
    public boolean replacesState() {
        return true;
    }
}
