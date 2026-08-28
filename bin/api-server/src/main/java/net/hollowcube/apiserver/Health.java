package net.hollowcube.apiserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

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

    /// Ready while the pool can hand out a connection the driver still considers usable, which is
    /// the only dependency this process has. A pool that is merely busy is not unready — waiting
    /// for a connection is normal — so what is bounded here is the driver's validation.
    public record Ready(DataSource dataSource) implements HttpHandler {
        private static final Logger logger = LoggerFactory.getLogger(Ready.class);
        private static final int VALIDATION_TIMEOUT_SECONDS = 2;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (var connection = dataSource.getConnection()) {
                exchange.sendResponseHeaders(connection.isValid(VALIDATION_TIMEOUT_SECONDS) ? 200 : 503, -1);
            } catch (SQLException e) {
                logger.info("ready check failed: {}", e.getMessage());
                exchange.sendResponseHeaders(503, -1);
            } finally {
                exchange.close();
            }
        }
    }

    private Health() {
    }
}
