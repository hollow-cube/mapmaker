package net.hollowcube.mapmaker.map.requests;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.MapSize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record MapCreateRequest(
        @NotNull String authorizer,
        boolean isOrg,
        @Nullable String owner,
        @NotNull MapSize size,
        int slot,
        int protocolVersion
) {

    public static @NotNull MapCreateRequest forOrg(
            @NotNull String authorizer,
            @NotNull String owner,
            @NotNull MapSize size,
            int protocolVersion
    ) {
        return new MapCreateRequest(authorizer, true, owner, size, -1, protocolVersion);
    }

    public static @NotNull MapCreateRequest forPlayer(
            @NotNull String player,
            @NotNull MapSize size,
            int slot,
            int protocolVersion
    ) {
        return new MapCreateRequest(player, false, player, size, slot, protocolVersion);
    }

}
