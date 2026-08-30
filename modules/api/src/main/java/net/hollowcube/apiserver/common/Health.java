package net.hollowcube.apiserver.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/// The two probes the deployment points at.
public final class Health {

    /// Alive as soon as there is a process to ask, which is all a liveness probe should mean: a
    /// database this cannot reach is not a reason to restart it.
    public record Alive() implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        }
    }

    /// Ready while every pool can hand out a connection the driver still considers usable and the
    /// NATS connection is up — which is every dependency these processes have. A pool that is merely
    /// busy is not unready — waiting for a connection is normal — so what is bounded here is the
    /// driver's validation.
    ///
    /// @param nats null in a process that publishes nothing
    public record Ready(List<DataSource> pools, @Nullable NatsPublisher nats) implements HttpHandler {
        private static final Logger logger = LoggerFactory.getLogger(Ready.class);
        private static final int VALIDATION_TIMEOUT_SECONDS = 2;

        public Ready(DataSource pool) {
            this(List.of(pool), null);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                exchange.sendResponseHeaders(ready() ? 200 : 503, -1);
            } finally {
                exchange.close();
            }
        }

        private boolean ready() {
            if (nats != null && !nats.connected()) {
                logger.info("ready check failed: nats is not connected");
                return false;
            }
            for (var pool : pools) {
                try (var connection = pool.getConnection()) {
                    if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) continue;
                    logger.info("ready check failed: a connection came back invalid");
                    return false;
                } catch (SQLException e) {
                    logger.info("ready check failed: {}", e.getMessage());
                    return false;
                }
            }
            return true;
        }
    }

    private Health() {
    }
}
