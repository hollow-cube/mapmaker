package net.hollowcube.ipc.chat;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/// One command a player ran.
///
/// @param timestamp  when it was submitted, off the sending server's clock
/// @param command    what was typed, without the leading slash
/// @param mapId      the map they were in, null in the hub
/// @param instanceId the world within that map, from the same place
/// @param remote     whether it was one of the commands the api executes rather than the server
/// @param durationMs how long dispatch took, the remote round trip included
@RuntimeGson
public record CommandExecution(
    Instant timestamp,
    String playerId,
    String serverId,
    @Nullable String mapId,
    @Nullable String instanceId,
    String command,
    boolean remote,
    CommandOutcome outcome,
    @Nullable String error,
    int durationMs
) {
}
