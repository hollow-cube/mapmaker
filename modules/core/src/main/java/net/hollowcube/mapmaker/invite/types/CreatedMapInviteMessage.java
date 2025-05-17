package net.hollowcube.mapmaker.invite.types;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record CreatedMapInviteMessage(@NotNull InviteType type, @NotNull String senderId, @NotNull String recipientId,
                                      @NotNull String mapId) {
}
