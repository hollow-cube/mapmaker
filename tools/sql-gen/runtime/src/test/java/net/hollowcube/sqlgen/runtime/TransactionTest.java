package net.hollowcube.sqlgen.runtime;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {

    @Test
    void commitFailureRollsBackAndDiscardsCallbacks() {
        var calls = new ArrayList<String>();
        var failure = new SQLException("commit rejected");
        var source = source(calls, failure);

        assertSame(failure, assertThrows(SQLException.class, () -> Transaction.run(source, tx -> {
            tx.afterCommit(() -> calls.add("callback"));
            return null;
        })));
        assertEquals(List.of("begin", "commit", "rollback", "restore", "close"), calls);
    }

    @Test
    void callbacksRunAfterConnectionRelease() {
        var calls = new ArrayList<String>();
        var result = Transaction.run(source(calls, null), tx -> {
            tx.afterCommit(() -> calls.add("callback"));
            return 42;
        });

        assertEquals(42, result);
        assertEquals(List.of("begin", "commit", "restore", "close", "callback"), calls);
    }

    private static DataSource source(List<String> calls, SQLException commitFailure) {
        var connection = (Connection) Proxy.newProxyInstance(TransactionTest.class.getClassLoader(),
            new Class<?>[]{Connection.class}, (_, method, args) -> {
                switch (method.getName()) {
                    case "getAutoCommit" -> { return true; }
                    case "setAutoCommit" -> calls.add((boolean) args[0] ? "restore" : "begin");
                    case "commit" -> {
                        calls.add("commit");
                        if (commitFailure != null) throw commitFailure;
                    }
                    case "rollback", "close" -> calls.add(method.getName());
                    default -> throw new UnsupportedOperationException(method.getName());
                }
                return null;
            });
        return (DataSource) Proxy.newProxyInstance(TransactionTest.class.getClassLoader(),
            new Class<?>[]{DataSource.class}, (_, method, _) -> {
                if (method.getName().equals("getConnection")) return connection;
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
