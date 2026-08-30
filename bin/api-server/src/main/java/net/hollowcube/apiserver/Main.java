package net.hollowcube.apiserver;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.common.Health;
import net.hollowcube.apiserver.common.Pools;
import net.hollowcube.apiserver.common.PostgresUri;
import net.hollowcube.apiserver.common.VaultSecrets;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.hdb.PostgresHeadDatabase;
import net.hollowcube.apiserver.session.PostgresSessions;
import net.hollowcube.ipc.hdb.HeadDatabaseServer;
import net.hollowcube.ipc.session.SessionServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/// The api server as a process: the pool, the port and the secrets they come from.
///
/// Everything it serves lives in `modules:api` and needs none of this, so the same services can be
/// constructed against any [javax.sql.DataSource] and called directly by a process that embeds them.
///
/// There is no authentication: this listens on the internal network only, and every handler here
/// answers the same questions the Go api-server's `/v4/internal` routes already answer without one.
public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int SHUTDOWN_SECONDS = 5;

    public static void main(String[] args) throws IOException {
        var secrets = VaultSecrets.load();
        // The head database is a table in the map service's schema, so this is the same uri, out of
        // the same vault key, that the Go api-server opens its `mapdb` pool on.
        var dataSource = Pools.postgres(PostgresUri.parse(secrets.require("postgres.maps_uri", "DATABASE_URL")), "api-server");
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
                new HeadDatabaseServer(new PostgresHeadDatabase(db), gson)),
            server.createContext(SessionServer.PATH,
                new SessionServer(new PostgresSessions(db), gson))
        )) context.getFilters().add(requestLog);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(SHUTDOWN_SECONDS)));
        server.start();
        logger.info("api-server listening on {}", port);
    }

    private Main() {
    }
}
