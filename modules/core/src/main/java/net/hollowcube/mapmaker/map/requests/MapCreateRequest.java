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
    private static final int SLOTS_V2_SLOT = -1;
    private static final int CONTEST_SLOT = 1_000_000;

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

    public static @NotNull MapCreateRequest forPlayerV2(
        @NotNull String player,
        @NotNull MapSize size,
        int protocolVersion
    ) {
        return new MapCreateRequest(player, false, player, size, SLOTS_V2_SLOT, protocolVersion);
    }

    public static @NotNull MapCreateRequest forContest(@NotNull String player, int protocolVersion) {
        // Map size is irrelevant it is always overwritten to be colossal.
        return new MapCreateRequest(player, false, player, MapSize.NORMAL, CONTEST_SLOT, protocolVersion);
    }

}
