package net.hollowcube.sqlgen.testing;

import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/// A pool of exactly one connection that is never actually returned to anything.
///
/// The analysis engine tests run on is single-user, so a test cannot be handed a real pool. This
/// gives a generated `Database` the [DataSource] it expects while keeping every statement on the one
/// connection the test also seeds and rolls back through.
public final class SingleConnectionDataSource implements DataSource {

    private final Connection conn;
    private final Connection handle;
    private @Nullable Savepoint savepoint;

    public SingleConnectionDataSource(Connection conn) {
        this(conn, false);
    }

    /// As above, but for a connection already inside the transaction a test rolls back: a
    /// transaction the code under test opens becomes a savepoint in it, its commit a release and
    /// its rollback a rollback to the savepoint, so nothing it does outlives the test.
    public SingleConnectionDataSource(Connection conn, boolean nested) {
        this.conn = conn;
        // Generated code closes the connection it borrowed, which for a pool means "give it back".
        // There is nothing to give it back to here, so close is the one call that gets swallowed.
        this.handle = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("close")) return null;
                if (nested) {
                    switch (method.getName()) {
                        case "getAutoCommit" -> {
                            return savepoint == null;
                        }
                        case "setAutoCommit" -> {
                            if (!(boolean) args[0] && savepoint == null) savepoint = conn.setSavepoint();
                            return null;
                        }
                        case "commit" -> {
                            if (savepoint != null) conn.releaseSavepoint(savepoint);
                            savepoint = null;
                            return null;
                        }
                        case "rollback" -> {
                            if (args == null || args.length == 0) {
                                if (savepoint != null) conn.rollback(savepoint);
                                savepoint = null;
                                return null;
                            }
                        }
                        default -> {
                        }
                    }
                }
                try {
                    return method.invoke(conn, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
    }

    /// The connection itself, for seeding and for transaction control the facade hides.
    public Connection connection() {
        return conn;
    }

    @Override
    public Connection getConnection() {
        return handle;
    }

    @Override
    public Connection getConnection(String username, String password) {
        return handle;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> type) throws SQLException {
        if (type.isInstance(this)) return type.cast(this);
        throw new SQLException("not a wrapper for " + type);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return type.isInstance(this);
    }
}
