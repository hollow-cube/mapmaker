package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NonBlocking
@NotNullByDefault
public interface PlayerState<S extends PlayerState<S, W>, W extends AbstractMapWorld2<S, W>> {

    default void configurePlayer(W world, Player player, @Nullable S lastState) {
        MapWorldHelpers.resetPlayerOnTickThread(player);
    }

    default void resetPlayer(W world, Player player, @Nullable S nextState) {
        // Nothing, we defensively reset by default on configure.
    }

    //todo can add some "handleConflict" method to deal with multiple states queued within a single tick

}
