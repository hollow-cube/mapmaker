package net.hollowcube.ipc.util;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.*;

class FailureGroupsTest {

    @Test
    void lostDatabaseGroupsAsOneIssueWhereverItWasHit() {
        var poolTimeout = new RuntimeException(
            new SQLTransientConnectionException(
                "api-server - Connection is not available",
                new SQLException("The connection attempt failed.", "08001")
            )
        );
        var adminShutdown = new IpcException(
            500,
            "upsert failed",
            new SQLException("terminating connection", "57P01")
        );

        assertEquals("postgres-unavailable", fingerprint(poolTimeout));
        assertEquals("postgres-unavailable", fingerprint(adminShutdown));
    }

    @Test
    void queryErrorsKeepDefaultGrouping() {
        assertNull(fingerprint(new SQLException("duplicate key", "23505")));
        assertNull(fingerprint(new SQLException("no state")));
    }

    @Test
    void failedCallsGroupByCallAndKind() {
        assertEquals(
            "ipc-5xx:/map/get",
            fingerprint(new IpcException(503, "/map/get", "answered 503", null))
        );
        assertEquals(
            "ipc-unreachable:/map/get",
            fingerprint(new IpcException(0, "/map/get", "failed", null))
        );
    }

    @Test
    void callerMistakesAndImplementationThrowsKeepDefaultGrouping() {
        assertNull(fingerprint(new IpcException(404, "/map/get", "answered 404", null)));
        assertNull(fingerprint(IpcException.internal("thrown by an implementation")));
    }

    @Test
    void existingFingerprintIsKept() {
        var event = new JsonObject();
        event.addProperty("$exception_fingerprint", "mine");
        FailureGroups.apply(new SQLTransientConnectionException("timeout"), event);
        assertEquals("mine", event.get("$exception_fingerprint").getAsString());
        assertFalse(event.has("$issue_name"));
    }

    private static String fingerprint(Throwable thrown) {
        var event = new JsonObject();
        FailureGroups.apply(thrown, event);
        return event.has("$exception_fingerprint")
            ? event.get("$exception_fingerprint").getAsString()
            : null;
    }
}
