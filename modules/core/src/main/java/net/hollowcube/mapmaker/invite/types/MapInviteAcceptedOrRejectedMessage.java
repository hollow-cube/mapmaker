package net.hollowcube.mapmaker.invite.types;

import org.jetbrains.annotations.NotNull;

public record MapInviteAcceptedOrRejectedMessage(@NotNull InviteType type, @NotNull String senderId,
                                                 @NotNull String recipientId, @NotNull String mapId, boolean accepted) {
}
