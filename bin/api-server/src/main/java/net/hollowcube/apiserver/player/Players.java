package net.hollowcube.apiserver.player;

import net.hollowcube.apiserver.common.Json;
import net.hollowcube.apiserver.db.PlayerData;
import net.hollowcube.ipc.player.PlayerStub;
import org.jetbrains.annotations.Nullable;

/// A `player_data` row as the game servers read it.
public final class Players {

    public static PlayerStub stub(PlayerData row) {
        return new PlayerStub(
            row.id(),
            row.username(),
            DisplayNames.of(row),
            Roles.flags(row.role(), row.hypercubeEnd()),
            Json.object(row.settings())
        );
    }

    public static net.hollowcube.ipc.player.@Nullable PlayerData data(@Nullable PlayerData row) {
        if (row == null) return null;
        var flags = Roles.flags(row.role(), row.hypercubeEnd());
        return new net.hollowcube.ipc.player.PlayerData(
            row.id(),
            row.username(),
            DisplayNames.of(row),
            Json.object(row.settings()),
            row.playtime(),
            row.cubits(),
            row.hypercubeEnd(),
            flags,
            Roles.mapSlots(row.extraMapSlots(), flags),
            Roles.maxMapSize(row.maxMapSize(), flags),
            Roles.builderSlots(row.mapBuilders(), flags),
            row.coins()
        );
    }

    private Players() {}
}
