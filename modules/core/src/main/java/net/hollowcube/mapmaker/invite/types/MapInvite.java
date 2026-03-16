package net.hollowcube.mapmaker.invite.types;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.entity.Player;

@RuntimeGson
public record MapInvite(
    InviteType inviteType,
    String senderId,
    String recipientId,
    String mapId,
    long time
) {

    public MapInvite(InviteType type, Player sender, String recipientId, MapData map) {
        this(type, sender.getUuid().toString(), recipientId, map.id(), System.currentTimeMillis());
    }
}
