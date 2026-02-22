package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;

public class FinishPlateBlock implements BlockHandler, PressurePlateBlock {
    private static final Key KEY = Key.key("mapmaker:finish_plate");

    public static final FinishPlateBlock INSTANCE = new FinishPlateBlock();

    private FinishPlateBlock() {
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public void onEnter(Collision collision) {
        final var player = collision.player();
        if (!(collision.world() instanceof ParkourMapWorld world)) return;

        var nextState = switch (world.getPlayerState(player)) {
            case ParkourState.Playing2(var saveState) -> {
                var finishState = saveState.copy(saveState.state(PlayState.class));
                finishState.complete(System.nanoTime() / 1_000_000);
                finishState.setEndLatency(((MapPlayer) player).averageLatency());
                yield new ParkourState.Finished(finishState);
            }
            case ParkourState.Testing(var _, var parent) -> {
                if (parent != null) yield parent;

                world.handleTestingModeFinish(player);
                yield null;
            }
            case null, default -> null;
        };
        if (nextState != null) world.changePlayerState(player, nextState);
    }

}
