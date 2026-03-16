package net.hollowcube.mapmaker.map.requests;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.MapSize;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record MapCreateRequest(
    String authorizer,
    boolean isOrg,
    @Nullable String owner,
    MapSize size,
    int slot,
    int protocolVersion
) {
    private static final int SLOTS_V2_SLOT = -1;
    private static final int CONTEST_SLOT = 1_000_000;

    public static MapCreateRequest forOrg(String authorizer, String owner, MapSize size, int protocolVersion) {
        return new MapCreateRequest(authorizer, true, owner, size, -1, protocolVersion);
    }

    public static MapCreateRequest forPlayer(String player, MapSize size, int slot, int protocolVersion) {
        return new MapCreateRequest(player, false, player, size, slot, protocolVersion);
    }

    public static MapCreateRequest forPlayerV2(String player, MapSize size, int protocolVersion) {
        return new MapCreateRequest(player, false, player, size, SLOTS_V2_SLOT, protocolVersion);
    }

    public static MapCreateRequest forContest(String player, int protocolVersion) {
        // Map size is irrelevant it is always overwritten to be colossal.
        return new MapCreateRequest(player, false, player, MapSize.NORMAL, CONTEST_SLOT, protocolVersion);
    }

}
