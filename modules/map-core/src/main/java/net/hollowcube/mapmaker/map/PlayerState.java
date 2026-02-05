package net.hollowcube.mapmaker.map;

import net.hollowcube.compat.noxesium.components.NoxesiumGameComponents;
import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NonBlocking
@NotNullByDefault
public interface PlayerState<S extends PlayerState<S, W>, W extends AbstractMapWorld<S, W>> {

    default void configurePlayer(W world, Player player, @Nullable S lastState) {
        MapWorldHelpers.resetPlayerOnTickThread(player);

        var noxesium = NoxesiumPlayer.get(player);
        noxesium.clear();
        noxesium.set(NoxesiumGameComponents.DISABLE_SPIN_ATTACK_COLLISIONS, true);
        noxesium.set(NoxesiumGameComponents.CLIENT_AUTHORITATIVE_ELYTRA, true);
    }

    default void resetPlayer(W world, Player player, @Nullable S nextState) {
        // Nothing, we defensively reset by default on configure.
    }

    default S handleConflict(S other) {
        return other;
    }

}
