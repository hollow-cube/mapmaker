package net.hollowcube.apiserver;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.hdb.PostgresHeadDatabase;
import net.hollowcube.ipc.hdb.HeadDatabaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/// The api server as a process: the pool, the port and the secrets they come from.
///
/// Everything it serves lives in `modules:api` and needs none of this, so the same services can be
/// constructed against any [DataSource] and called directly by a process that embeds them.
///
/// There is no authentication: this listens on the internal network only, and every handler here
/// answers the same questions the Go api-server's `/v4/internal` routes already answer without one.
public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int SHUTDOWN_SECONDS = 5;

    public static void main(String[] args) throws IOException {
        var secrets = VaultSecrets.load();
        var dataSource = dataSource(secrets);
        var db = new ApiDatabase(dataSource);
        var gson = new Gson();

        var port = Integer.parseInt(secrets.get("http.port", "PORT", "9124"));
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        // Every handler is one database call deep and blocks for all of it, which is what virtual
        // threads are for.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        var requestLog = new RequestLog();
        for (var context : List.of(
            server.createContext("/alive", new Health.Alive()),
            server.createContext("/ready", new Health.Ready(dataSource)),
            server.createContext(HeadDatabaseServer.PATH,
                new HeadDatabaseServer(new PostgresHeadDatabase(db), gson))
        )) context.getFilters().add(requestLog);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(SHUTDOWN_SECONDS)));
        server.start();
        logger.info("api-server listening on {}", port);
    }

    private static DataSource dataSource(VaultSecrets secrets) {
        // The head database is a table in the map service's schema, so this is the same uri, out of
        // the same vault key, that the Go api-server opens its `mapdb` pool on.
        var uri = PostgresUri.parse(secrets.require("postgres.maps_uri", "DATABASE_URL"));

        var config = new HikariConfig();
        config.setJdbcUrl(uri.jdbcUrl());
        config.setUsername(uri.user());
        config.setPassword(uri.password());
        config.setPoolName("api-server");
        // Hikari waits 30s for a connection by default, which is longer than /ready is worth
        // keeping a probe waiting and longer than an internal call should hang before it fails.
        config.setConnectionTimeout(5_000);
        // A database that is briefly unreachable at startup is something to report through /ready,
        // not to crash over, so the pool fills lazily rather than failing construction.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private Main() {
    }
}
