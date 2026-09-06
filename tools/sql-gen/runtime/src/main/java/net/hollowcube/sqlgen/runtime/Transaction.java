package net.hollowcube.sqlgen.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/// Runs a block of statements on one connection, committing on return and rolling back if the
/// block or commit throws. After-commit callbacks run outside that rollback boundary.
///
/// A single statement is already atomic under autocommit, so this exists only for the invariants
/// that span more than one — which is also why nesting is refused rather than silently flattened
/// into the outer transaction's commit.
public final class Transaction {

    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private final Connection conn;
    private final List<Runnable> afterCommit = new ArrayList<>();
    private boolean open = true;

    private Transaction(Connection conn) {
        this.conn = conn;
    }

    public Connection conn() {
        return conn;
    }

    /// Runs synchronously after commit and connection release, in registration order. Rollback
    /// discards callbacks. A callback that throws is logged and the rest still run: the
    /// transaction is already committed, so there is nothing to roll back and nothing a caller
    /// could do with the failure except retry a write that has already happened.
    public void afterCommit(Runnable callback) {
        if (!open) throw new IllegalStateException("transaction has already completed");
        afterCommit.add(Objects.requireNonNull(callback, "callback"));
    }

    public static <R> R run(DataSource dataSource, Function<Transaction, R> work) {
        if (ACTIVE.get()) {
            throw new IllegalStateException("already in a transaction; nested transactions are not supported");
        }
        Transaction tx;
        R result;
        try (Connection conn = dataSource.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            tx = new Transaction(conn);
            ACTIVE.set(true);
            try {
                result = work.apply(tx);
                conn.commit();
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
                tx.open = false;
                ACTIVE.remove();
                // The connection is going back to a pool that expects to find it as it left it.
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw Sneaky.rethrow(e);
        }
        tx.runAfterCommit();
        return result;
    }

    private void runAfterCommit() {
        for (var callback : afterCommit) {
            try {
                callback.run();
            } catch (RuntimeException e) {
                logger.error("after-commit callback failed", e);
            }
        }
    }
}
