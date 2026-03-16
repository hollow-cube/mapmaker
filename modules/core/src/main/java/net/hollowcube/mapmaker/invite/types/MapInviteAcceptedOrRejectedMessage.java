package net.hollowcube.mapmaker.invite.types;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record MapInviteAcceptedOrRejectedMessage(
    InviteType type,
    String senderId,
    String recipientId,
    String mapId,
    boolean accepted
) {
}
