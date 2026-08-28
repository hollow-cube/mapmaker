package net.hollowcube.sqlgen.runtime;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/// Where a generated query group gets the connection it runs one statement on.
///
/// The two implementations differ only in what [#release] means: a pool-backed source closes the
/// connection (returning it to the pool), while a source pinned to a transaction leaves it open for
/// the next statement. Generated code always acquires and releases around a single statement, so it
/// never needs to know which it is talking to.
public interface ConnectionSource {

    Connection acquire() throws SQLException;

    void release(Connection conn) throws SQLException;

    /// A source that borrows a fresh connection per statement, in autocommit.
    static ConnectionSource pooled(DataSource dataSource) {
        return new ConnectionSource() {
            @Override
            public Connection acquire() throws SQLException {
                return dataSource.getConnection();
            }

            @Override
            public void release(Connection conn) throws SQLException {
                conn.close();
            }
        };
    }

    /// A source that hands out the same connection every time and never closes it. The caller owns
    /// the connection's lifetime and its transaction state.
    static ConnectionSource pinned(Connection conn) {
        return new ConnectionSource() {
            @Override
            public Connection acquire() {
                return conn;
            }

            @Override
            public void release(Connection ignored) {
            }
        };
    }
}
