package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.ipc.chat.ChatService;
import net.hollowcube.ipc.hdb.HeadDatabaseService;
import net.hollowcube.ipc.replay.ReplayService;
import org.jetbrains.annotations.NotNull;

/// Everything this server calls on the api over ipc.
///
/// The interfaces rather than the generated clients, so that a process which happens to host an
/// implementation itself can hand the implementation over and skip the socket. See
/// [AbstractMapServer#createIpcServices].
public record IpcServices(
    @NotNull HeadDatabaseService headDatabase,
    @NotNull ChatService chat,
    @NotNull ReplayService replays
) {
}
