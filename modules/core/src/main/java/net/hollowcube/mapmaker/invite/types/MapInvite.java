package net.hollowcube.mapmaker.invite.types;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record MapInvite(@NotNull InviteType inviteType, @NotNull String senderId, @NotNull String recipientId,
                        @NotNull String mapId, long time) {

    public MapInvite(@NotNull InviteType type, @NotNull Player sender, @NotNull String recipientId, @NotNull MapData map) {
        this(type, sender.getUuid().toString(), recipientId, map.id(), System.currentTimeMillis());
    }
}
