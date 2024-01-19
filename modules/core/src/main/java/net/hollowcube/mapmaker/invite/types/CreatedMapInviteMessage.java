package net.hollowcube.mapmaker.invite.types;

import org.jetbrains.annotations.NotNull;

public record CreatedMapInviteMessage(@NotNull InviteType type, @NotNull String senderId, @NotNull String recipientId,
                                      @NotNull String mapId) {
}
