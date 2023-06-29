package net.hollowcube.mapmaker.map;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record LeaderboardData(
        @NotNull List<Entry> top,
        @Nullable Entry player
) {

    public record Entry(@NotNull String player, long score, int rank) {}
}
