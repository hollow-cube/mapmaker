package net.hollowcube.ipc.session;

import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

/// Player sessions: the one row per player online anywhere on the network. The rest of the Go
/// `/v3/internal/session` surface lands here as it is ported.
@Ipc
public interface SessionService {

    /// How many players are online across every proxy and server, which is what a server list ping
    /// shows: a single proxy only knows its own.
    int onlinePlayers();

    /// A ready hub other than `exclude`, or null when there is none.
    @Nullable
    GameServer findHub(@Nullable String exclude);

    /// The server `id` names while its pod is still around, or null once it is gone.
    @Nullable
    GameServer findServer(String id);

}
