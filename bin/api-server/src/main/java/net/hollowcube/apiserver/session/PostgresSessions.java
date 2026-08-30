package net.hollowcube.apiserver.session;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.session.SessionService;

/// Player sessions, served out of the `player_sessions` table the Go api-server writes.
public final class PostgresSessions implements SessionService {

    private final ApiDatabase db;

    public PostgresSessions(ApiDatabase db) {
        this.db = db;
    }

    @Override
    public int onlinePlayers() {
        // Every row, hidden players and all, the way the player count graphs count them.
        return (int) db.sessions.countPlayerSessions();
    }
}
