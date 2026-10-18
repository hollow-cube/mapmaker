package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

public class FinishPlateBlock implements BlockHandler, PressurePlateBlock, Trigger {
    private static final Key KEY = Key.key("mapmaker:finish_plate");

    public static final FinishPlateBlock INSTANCE = new FinishPlateBlock();

    private FinishPlateBlock() {
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Trigger createTrigger(BlockVec position, Block block) {
        return this;
    }

    @Override
    public void onEnter(MapWorld mapWorld, Player player) {
        if (!(mapWorld instanceof ParkourMapWorld world)) return;

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

}
