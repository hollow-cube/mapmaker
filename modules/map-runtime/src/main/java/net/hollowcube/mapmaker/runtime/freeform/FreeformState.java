package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.mapmaker.map.PlayerState;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public sealed interface FreeformState extends PlayerState<FreeformState, FreeformMapWorld> {

    record Playing() implements FreeformState {

        @Override
        public void configurePlayer(
            FreeformMapWorld world,
            Player player,
            @Nullable FreeformState lastState
        ) {
            FreeformState.super.configurePlayer(world, player, lastState);
            world.runtime().configurePlaying(world, player);
        }

        @Override
        public void resetPlayer(
            FreeformMapWorld world,
            Player player,
            @Nullable FreeformState nextState
        ) {
            world.runtime().resetPlaying(world, player);
        }
    }
}
