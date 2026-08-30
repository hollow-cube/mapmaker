package net.hollowcube.apiserver;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.chat.ChatServiceImpl;
import net.hollowcube.apiserver.common.Health;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.common.Pools;
import net.hollowcube.apiserver.common.PostgresUri;
import net.hollowcube.apiserver.common.VaultSecrets;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.hdb.HeadDatabaseServiceImpl;
import net.hollowcube.apiserver.session.SessionServiceImpl;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.chat.ChatServer;
import net.hollowcube.ipc.hdb.HeadDatabaseServer;
import net.hollowcube.ipc.session.SessionServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/// The api server as a process: the pools, the port and the secrets they come from.
///
/// Everything it serves needs none of this, so the same services can be constructed against any
/// [javax.sql.DataSource] and called directly by a process that embeds them.
///
/// There is no authentication: this listens on the internal network only, and every handler here
/// answers the same questions the Go api-server's `/v4/internal` routes already answer without one.
public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int SHUTDOWN_SECONDS = 5;

    public static void main(String[] args) throws IOException {
        var secrets = VaultSecrets.load();
        // One pool for what Go opens three on: `postgres.uri`, `postgres.maps_uri` and
        // `postgres.players_uri` are the same url, so head_db, jobs, chat_messages, player_sessions,
        // command_log, player_data and punishments are all in it.
        var pool = Pools.postgres(PostgresUri.parse(secrets.require("postgres.maps_uri", "DATABASE_URL")), "api-server");
        var db = new ApiDatabase(pool);

        // The same vault key the Go api-server reads, so the two publish onto one cluster.
        var nats = NatsPublisher.connect(secrets.get("nats.servers", "NATS_SERVERS", "nats://localhost:4222"), Wire.gson());

        var port = Integer.parseInt(secrets.get("http.port", "PORT", "9124"));
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        // Every handler is a database call or two deep and blocks for all of it, which is what
        // virtual threads are for.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        var requestLog = new RequestLog();
        for (var context : List.of(
            server.createContext("/alive", new Health.Alive()),
            server.createContext("/ready", new Health.Ready(List.of(pool), nats)),
            server.createContext(HeadDatabaseServer.PATH,
                new HeadDatabaseServer(new HeadDatabaseServiceImpl(db))),
            server.createContext(SessionServer.PATH,
                new SessionServer(new SessionServiceImpl(db))),
            server.createContext(ChatServer.PATH,
                new ChatServer(new ChatServiceImpl(db, nats)))
        )) context.getFilters().add(requestLog);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(SHUTDOWN_SECONDS);
            nats.close();
        }));
        server.start();
        logger.info("api-server listening on {}", port);
    }

    private Main() {
    }
}
