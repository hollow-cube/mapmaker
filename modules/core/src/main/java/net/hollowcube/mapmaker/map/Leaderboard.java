package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record Leaderboard(
    boolean asc,
    @NotNull Format format,
    @NotNull String score
) {
    public enum Format {
        TIME,
        NUMBER,
        PERCENT,
    }
}
