package net.hollowcube.ipc.map;

import java.util.UUID;

/// One of a player's best times, ranked against the map's board.
///
/// @param completionTime playtime rounded to the tick, which is what the rank was computed from
public record PlayerTopTime(
    UUID mapId,
    String publishedId,
    String mapName,
    long completionTime,
    int rank
) {}
