package net.hollowcube.sqlgen.runtime;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/// Runs a block of statements on one connection, committing on return and rolling back on any throw.
///
/// A single statement is already atomic under autocommit, so this exists only for the invariants
/// that span more than one — which is also why nesting is refused rather than silently flattened
/// into the outer transaction's commit.
public final class Transaction {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    public static <R> R run(DataSource dataSource, Function<Connection, R> work) {
        if (ACTIVE.get()) {
            throw new IllegalStateException("already in a transaction; nested transactions are not supported");
        }
        try (Connection conn = dataSource.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            ACTIVE.set(true);
            try {
                R result = work.apply(conn);
                conn.commit();
                return result;
            } catch (Throwable t) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    // Whatever went wrong first is the useful one; losing it to a failed rollback
                    // would be the second-worst outcome here.
                    t.addSuppressed(e);
                }
                throw t;
            } finally {
                ACTIVE.remove();
                // The connection is going back to a pool that expects to find it as it left it.
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw Sneaky.rethrow(e);
        }
    }

    private Transaction() {
    }
}
