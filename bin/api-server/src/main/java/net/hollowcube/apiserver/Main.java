package net.hollowcube.apiserver;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.anticheat.AnticheatServiceImpl;
import net.hollowcube.apiserver.anticheat.AnticheatTraceStore;
import net.hollowcube.apiserver.chat.ChatServiceImpl;
import net.hollowcube.apiserver.common.Drain;
import net.hollowcube.apiserver.common.Health;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.common.Pools;
import net.hollowcube.apiserver.common.PostHogIds;
import net.hollowcube.apiserver.common.PostgresUri;
import net.hollowcube.apiserver.common.VaultSecrets;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.hdb.HeadDatabaseServiceImpl;
import net.hollowcube.apiserver.map.MapServiceImpl;
import net.hollowcube.apiserver.notification.NotificationServiceImpl;
import net.hollowcube.apiserver.player.PlayerServiceImpl;
import net.hollowcube.apiserver.player.SocialServiceImpl;
import net.hollowcube.apiserver.replay.ReplayServiceImpl;
import net.hollowcube.apiserver.s3.HttpS3Client;
import net.hollowcube.apiserver.session.SessionServiceImpl;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.anticheat.AnticheatServer;
import net.hollowcube.ipc.chat.ChatServer;
import net.hollowcube.ipc.hdb.HeadDatabaseServer;
import net.hollowcube.ipc.map.MapServer;
import net.hollowcube.ipc.notification.NotificationServer;
import net.hollowcube.ipc.player.PlayerServer;
import net.hollowcube.ipc.player.SocialServer;
import net.hollowcube.ipc.replay.ReplayServer;
import net.hollowcube.ipc.session.SessionServer;
import net.hollowcube.ipc.util.IpcFailures;
import net.hollowcube.posthog.PostHog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    /// Draining ([Drain]) comes first and stopping second: `HttpServer.stop` alone leaves every
    /// caller with a pooled connection holding a request that will never be answered.
    private static final int DRAIN_SECONDS = 5;
    private static final int SHUTDOWN_SECONDS = 5;

    /// The trace volume (ReadWriteOnce, which is what holds this at one replica pinned to its
    /// node), and the most one capture trace may be. The cap is a refusal rather than a
    /// truncation: half a trace is not evidence, and the proxy that shipped it keeps its copy.
    private static final String TRACE_DIR = "/data/anticheat";
    private static final long MAX_TRACE_BYTES = 512L << 20;

    /// Hardcoded in the Go api-server too; both write the same objects into it.
    private static final String REPLAY_BUCKET = "mapmaker-replays";
    private static final String MAP_BUCKET = "mapmaker";

    /// The Go api-server proxies PostHog, and the game servers go through it; so does this.
    private static final String POSTHOG_PROXY = "http://api-server.mapmaker:9124/posthog";

    public static void main(String[] args) throws IOException {
        var secrets = VaultSecrets.load();
        // A process with no vault secret is a local one, and the uninitialised client drops
        // everything, which is what a local run wants. The endpoint is not a key the secret
        // carries, so it is not what decides.
        if (secrets.present() || System.getenv("POSTHOG_ENDPOINT") != null) {
            PostHog.init(
                PostHogIds.PROJECT_KEY,
                config -> config.endpoint(
                    secrets.get("posthog.endpoint", "POSTHOG_ENDPOINT", POSTHOG_PROXY)
                )
            );
        } else {
            logger.info("no vault secret and no POSTHOG_ENDPOINT, so posthog events go nowhere");
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            logger.error("uncaught exception in {}", thread.getName(), e);
            PostHog.captureException(e, null, Map.of("thread", thread.getName()));
        });
        // A 500 is already in the log; this is what puts it in front of someone.
        IpcFailures.reportTo((path, e) -> PostHog.captureException(e, null, Map.of("ipc", path)));

        // One pool for what Go opens three on: `postgres.uri`, `postgres.maps_uri` and
        // `postgres.players_uri` are the same url, so head_db, jobs, chat_messages, player_sessions,
        // command_log, player_data and punishments are all in it.
        var pool = Pools.postgres(
            PostgresUri.parse(secrets.require("postgres.maps_uri", "DATABASE_URL")),
            "api-server"
        );
        var db = new ApiDatabase(pool);

        // The same vault key the Go api-server reads, so the two publish onto one cluster.
        // Defaulted only where there is no vault secret, which is what says this is a local run.
        // In the cluster a missing broker has to be a startup failure: the client reconnects for
        // ever, so a wrong default is an api that looks up and silently publishes into nothing.
        var natsServers = secrets.present()
            ? secrets.require("nats.servers", "NATS_SERVERS")
            : secrets.get("nats.servers", "NATS_SERVERS", "nats://localhost:4222");
        var nats = NatsPublisher.connect(natsServers, Wire.gson());

        // The same `s3.*` keys Go's config reads out of the vault secret. Required rather than
        // defaulted: the development server builds its own client against local minio.
        var s3 = new HttpS3Client(
            HttpClient.newHttpClient(),
            secrets.require("s3.endpoint", "S3_ENDPOINT"),
            REPLAY_BUCKET,
            secrets.get("s3.region", "S3_REGION", "auto"),
            secrets.require("s3.access_key", "S3_ACCESS_KEY"),
            secrets.require("s3.secret_key", "S3_SECRET_KEY")
        );

        var mapWorlds = new HttpS3Client(
            HttpClient.newHttpClient(),
            secrets.require("s3.endpoint", "S3_ENDPOINT"),
            MAP_BUCKET,
            secrets.get("s3.region", "S3_REGION", "auto"),
            secrets.require("s3.access_key", "S3_ACCESS_KEY"),
            secrets.require("s3.secret_key", "S3_SECRET_KEY")
        );
        var redis = Pools.redis(secrets.require("redis.address", "REDIS_ADDRESS"));
        var minimumBuild = secrets.get(
            "maps.minimum_build_seconds",
            "MAP_MIN_BUILD_SECONDS",
            "1800"
        );
        var maps = new MapServiceImpl(
            db,
            mapWorlds,
            nats,
            redis,
            PostHog.getClient(),
            Duration.ofSeconds(Long.parseLong(minimumBuild))
        );

        var port = Integer.parseInt(secrets.get("http.port", "PORT", "9124"));
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        // Every handler is a database call or two deep and blocks for all of it, which is what
        // virtual threads are for.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        var notifications = new NotificationServiceImpl(db, nats);

        var requestLog = new RequestLog();
        var drain = new Drain();
        for (var context : List.of(
            server.createContext("/alive", new Health.Alive()),
            server.createContext("/ready", new Health.Ready(List.of(pool), nats, drain::draining)),
            server.createContext(
                HeadDatabaseServer.PATH,
                new HeadDatabaseServer(new HeadDatabaseServiceImpl(db))
            ),
            server.createContext(SessionServer.PATH, new SessionServer(new SessionServiceImpl(db))),
            server.createContext(ChatServer.PATH, new ChatServer(new ChatServiceImpl(db, nats))),
            server.createContext(
                AnticheatServer.PATH,
                new AnticheatServer(
                    new AnticheatServiceImpl(
                        db,
                        new AnticheatTraceStore(
                            Path.of(
                                secrets.get("anticheat.store_dir", "ANTICHEAT_STORE_DIR", TRACE_DIR)
                            ),
                            MAX_TRACE_BYTES
                        )
                    )
                )
            ),
            server.createContext(MapServer.PATH, new MapServer(maps)),
            server.createContext(
                ReplayServer.PATH,
                new ReplayServer(new ReplayServiceImpl(db, s3))
            ),
            server.createContext(PlayerServer.PATH, new PlayerServer(new PlayerServiceImpl(db))),
            server.createContext(
                SocialServer.PATH,
                new SocialServer(new SocialServiceImpl(db, notifications))
            ),
            server.createContext(NotificationServer.PATH, new NotificationServer(notifications))
        ))
            context.getFilters().addAll(List.of(requestLog, drain));

        Runtime.getRuntime().addShutdownHook(
            new Thread(
                () -> {
                    logger.info("draining for {}s before stopping", DRAIN_SECONDS);
                    drain.begin();
                    try {
                        Thread.sleep(Duration.ofSeconds(DRAIN_SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    server.stop(SHUTDOWN_SECONDS);
                    redis.close();
                    nats.close();
                    PostHog.shutdown();
                }
            )
        );
        server.start();
        logger.info("api-server listening on {}", port);
    }

    private Main() {}
}
