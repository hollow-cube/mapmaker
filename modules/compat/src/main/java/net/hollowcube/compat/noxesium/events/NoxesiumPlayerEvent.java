package net.hollowcube.compat.noxesium.events;

import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

public interface NoxesiumPlayerEvent extends PlayerEvent {

    NoxesiumPlayer noxesiumPlayer();

    @Override
    default Player getPlayer() {
        return noxesiumPlayer().player();
    }
}
