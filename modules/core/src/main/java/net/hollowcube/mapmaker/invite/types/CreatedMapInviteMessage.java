package net.hollowcube.mapmaker.invite.types;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record CreatedMapInviteMessage(
    InviteType type,
    String senderId,
    String recipientId,
    String mapId
) {
}
