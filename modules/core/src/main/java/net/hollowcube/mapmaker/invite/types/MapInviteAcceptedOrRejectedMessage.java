package net.hollowcube.mapmaker.invite.types;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record MapInviteAcceptedOrRejectedMessage(@NotNull InviteType type, @NotNull String senderId,
                                                 @NotNull String recipientId, @NotNull String mapId, boolean accepted) {
}
