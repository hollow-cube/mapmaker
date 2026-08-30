package net.hollowcube.command;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/// Called once for every command a player ran, after they have been told how it went.
///
/// Fired at the one place commands are dispatched, so whoever wants to see them does not have to
/// find every command. Rejections that never reached dispatch — the rate limit, a client that
/// cannot send commands at all — are not executions and are not called for.
///
/// @param input         what was typed, without the leading slash
/// @param durationNanos how long dispatch took, which for a command the api executes includes the
///                      round trip to it
public record CommandExecutedEvent(
    @NotNull Player player,
    @NotNull String input,
    @NotNull CommandResult result,
    long durationNanos
) implements PlayerEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
