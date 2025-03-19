package net.hollowcube.mapmaker.map.requests;

import net.hollowcube.mapmaker.map.MapSize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MapCreateRequest(
        @NotNull String authorizer,
        boolean isOrg,
        @Nullable String owner,
        @NotNull MapSize size,
        int slot
) {

    public static @NotNull MapCreateRequest forOrg(
            @NotNull String authorizer,
            @NotNull String owner,
            @NotNull MapSize size
    ) {
        return new MapCreateRequest(authorizer, true, owner, size, -1);
    }

    public static @NotNull MapCreateRequest forPlayer(
            @NotNull String player,
            @NotNull MapSize size,
            int slot
    ) {
        return new MapCreateRequest(player, false, player, size, slot);
    }

}
