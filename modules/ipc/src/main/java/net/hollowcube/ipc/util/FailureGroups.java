package net.hollowcube.ipc.util;

import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

/// PostHog groups an exception into an issue by its in-app frames, so a failure whose cause is outside
/// the code that hit it (the database or the api-server going away) becomes one issue per call site.
/// These give such failures a fingerprint of their cause instead; everything else keeps the default.
public final class FailureGroups {
    private static final String FINGERPRINT = "$exception_fingerprint";
    private static final String ISSUE_NAME = "$issue_name";

    /// Sets the fingerprint and issue name on a PostHog `$exception` event, if `thrown` is one of these.
    public static void apply(Throwable thrown, JsonObject event) {
        if (event.has(FINGERPRINT)) return;

        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (isDatabaseUnavailable(cause)) {
                event.addProperty(FINGERPRINT, "postgres-unavailable");
                event.addProperty(ISSUE_NAME, "Postgres unavailable");
                return;
            }
        }

        // A 4xx is the caller's mistake, which is exactly what call site grouping is for.
        if (thrown instanceof IpcException ipc
            && ipc.call() != null
            && (ipc.status() == 0 || ipc.status() >= 500)) {
            String kind = ipc.status() == 0 ? "unreachable" : "5xx";
            event.addProperty(FINGERPRINT, "ipc-" + kind + ":" + ipc.call());
            event.addProperty(ISSUE_NAME, "ipc " + ipc.call() + " " + kind);
        }
    }

    /// Hikari's pool timeout, or a SQLState for a lost connection (class 08) or a server going down
    /// (57P01 admin shutdown, 57P02 crash shutdown, 57P03 cannot connect now).
    private static boolean isDatabaseUnavailable(Throwable thrown) {
        if (thrown instanceof SQLTransientConnectionException) return true;
        if (!(thrown instanceof SQLException sql) || sql.getSQLState() == null) return false;
        String state = sql.getSQLState();
        return state.startsWith("08")
            || state.equals("57P01")
            || state.equals("57P02")
            || state.equals("57P03");
    }

    private FailureGroups() {}
}
