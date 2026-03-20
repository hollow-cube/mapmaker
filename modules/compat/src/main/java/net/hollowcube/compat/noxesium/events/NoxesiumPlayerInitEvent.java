package net.hollowcube.compat.noxesium.events;

import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.hollowcube.compat.noxesium.packets.v3.ServerboundHandshakeAcknowledgePacket;

public record NoxesiumPlayerInitEvent(
    NoxesiumPlayer noxesiumPlayer,
    ServerboundHandshakeAcknowledgePacket packet
) implements NoxesiumPlayerEvent {

}
