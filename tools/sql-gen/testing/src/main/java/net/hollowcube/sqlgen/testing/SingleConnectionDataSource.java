package net.hollowcube.sqlgen.testing;

import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Set;
import java.util.logging.Logger;
import javax.sql.DataSource;

/// A pool of exactly one connection that is never actually returned to anything.
///
/// The analysis engine tests run on is single-user, so a test cannot be handed a real pool. This
/// gives a generated `Database` the [DataSource] it expects while keeping every statement on the one
/// connection the test also seeds and rolls back through.
public final class SingleConnectionDataSource implements DataSource {

    private static final Set<String> TRANSACTION_CONTROL =
        Set.of("setAutoCommit", "commit", "rollback", "setSavepoint", "releaseSavepoint");

    private final Connection conn;
    private final Connection handle;

    public SingleConnectionDataSource(Connection conn) {
        this(conn, null);
    }

    /// As above, but refusing every transaction-control call with `refuseTransactions` as the
    /// message. [TestDb] uses this in rollback mode, where a commit would escape the rollback that
    /// is supposed to undo the test.
    public SingleConnectionDataSource(Connection conn, @Nullable String refuseTransactions) {
        this.conn = conn;
        // Generated code closes the connection it borrowed, which for a pool means "give it back".
        // There is nothing to give it back to here, so close is the one call that gets swallowed.
        this.handle = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("close")) return null;
                if (refuseTransactions != null && TRANSACTION_CONTROL.contains(method.getName())) {
                    throw new IllegalStateException(refuseTransactions);
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
