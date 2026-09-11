package net.hollowcube.ipc.map;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.ipc.util.JsonValueAdapter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/// A save state as stored. `state` is the edit or play state by [#type], and null once the state
/// is completed: a finished run keeps its numbers and drops where the player was standing.
///
/// @param score null until completed; a completed run without one scored by playtime, which is
///              what every run did before leaderboards were configurable
public record SaveStateData(
    UUID id,
    UUID mapId,
    UUID playerId,
    SaveStateType type,
    Instant created,
    Instant lastModified,
    int dataVersion,
    int protocolVersion,
    long playtime,
    long ticks,
    int resets,
    long totalPlaytime,
    boolean completed,
    @Nullable Double score,
    @JsonAdapter(JsonValueAdapter.class) @Nullable JsonObject state
) {}
