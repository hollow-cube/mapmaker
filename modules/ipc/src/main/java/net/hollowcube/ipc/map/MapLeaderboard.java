package net.hollowcube.ipc.map;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record MapLeaderboard(boolean asc, Format format, String score) {
    public enum Format {
        TIME,
        PERCENT,
        NUMBER,
        UNKNOWN,
    }

    public static final MapLeaderboard DEFAULT = new MapLeaderboard(
        true,
        Format.TIME,
        "q.playtime"
    );

    public MapLeaderboard withAsc(boolean asc) {
        return new MapLeaderboard(asc, format, score);
    }

    public MapLeaderboard withFormat(Format format) {
        return new MapLeaderboard(asc, format, score);
    }

    public MapLeaderboard withScore(String score) {
        return new MapLeaderboard(asc, format, score);
    }
}
