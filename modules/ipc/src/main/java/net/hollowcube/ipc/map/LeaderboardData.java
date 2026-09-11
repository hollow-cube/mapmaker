package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/// The top of a board and, when asked for, where one player stands on it.
///
/// A rank is one more than the number of strictly better scores, so tied players share one and the
/// next rank skips; a time board compares scores rounded to the tick first. The rule is the same
/// for the top entries and for `player`, whether or not they made the top.
///
/// @param player null when the player was not asked for or has no score; on a global board the
///               rank is -1, which is the one place it is not computed
public record LeaderboardData(List<Entry> top, @Nullable Entry player) {

    public static final LeaderboardData EMPTY = new LeaderboardData(List.of(), null);

    public record Entry(UUID player, long score, int rank) {}
}
