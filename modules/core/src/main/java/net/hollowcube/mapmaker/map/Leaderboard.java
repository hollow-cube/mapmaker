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

    public static final Leaderboard DEFAULT = new Leaderboard(true, Format.TIME, "q.playtime");

    public Leaderboard withAsc(boolean asc) {
        return new Leaderboard(asc, this.format, this.score);
    }

    public Leaderboard withFormat(Format format) {
        return new Leaderboard(this.asc, format, this.score);
    }

    public Leaderboard withScore(String score) {
        return new Leaderboard(this.asc, this.format, score);
    }
}
