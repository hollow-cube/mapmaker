package net.hollowcube.mapmaker.local.scripting;

import net.hollowcube.mapmaker.map.PlayerState;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public sealed interface FreeformState extends PlayerState<FreeformState, FreeformMapWorld> {

    record Building() implements FreeformState {

        @Override
        public void configurePlayer(FreeformMapWorld world, Player player, @Nullable FreeformState lastState) {
            FreeformState.super.configurePlayer(world, player, lastState);
        }

    }
}
