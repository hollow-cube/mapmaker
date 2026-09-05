package net.hollowcube.apiworker;

import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.apiserver.common.Pools;
import net.hollowcube.apiserver.common.PostHogIds;
import net.hollowcube.apiserver.common.PostgresUri;
import net.hollowcube.apiserver.common.VaultSecrets;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiworker.job.Worker;
import net.hollowcube.apiworker.jobs.CompactReplayRunner;
import net.hollowcube.apiworker.jobs.IndexMapRunner;
import net.hollowcube.apiworker.jobs.PlayerCountRunner;
import net.hollowcube.apiworker.jobs.ReconcileReplaysRunner;
import net.hollowcube.ipc.replay.ReplayClient;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.posthog.PostHog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;

/// The api worker as a process: the api's background work, off the path of its requests.
///
/// It listens on nothing and takes no arguments. Work is rows in the `jobs` table — recurring ones
/// it creates for itself, one-shot ones the api-server (or a human with psql) inserts — and the
/// table is also where to look at what it is doing. Kubernetes restarts it if the process dies,
/// which is all a probe could tell it.
///
/// It reads the same vault secret as the api-server, under the same service account, so the
/// database it opens is named by the same key.
public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    /// Inside kubernetes' default 30s termination grace, with room for the pool to close after.
    private static final Duration STOP_GRACE = Duration.ofSeconds(20);
    /// The Go api-server in the cluster, which is where map worlds come from; and under tilt,
    /// where a local run finds it.
    private static final String API_SERVER = "http://api-server.mapmaker:9124";
    private static final String LOCAL_API_SERVER = "http://localhost:9127";
    /// The java api-server, the same root every other ipc client in the deployment is built on.
    private static final String IPC_SERVICE_URL = "http://api-server-java:9124";
    private static final String LOCAL_IPC_SERVICE_URL = "http://localhost:9124";
    /// The Go api-server proxies PostHog, and the game servers go through it; so does this.
    private static final String POSTHOG_PROXY = API_SERVER + "/posthog";

    public static void main(String[] args) {
        var secrets = VaultSecrets.load();
        var dataSource = Pools.postgres(PostgresUri.parse(secrets.require("postgres.maps_uri", "DATABASE_URL")), "api-worker");
        var db = new ApiDatabase(dataSource);

        // A process with no vault secret is a local one, and the uninitialised client drops
        // everything, which is what a local run wants: the Go server likewise sends nothing under
        // tilt. The endpoint is not a key the secret carries, so it is not what decides.
        if (secrets.present() || System.getenv("POSTHOG_ENDPOINT") != null) {
            PostHog.init(PostHogIds.PROJECT_KEY, config -> config.endpoint(secrets.get("posthog.endpoint", "POSTHOG_ENDPOINT", POSTHOG_PROXY)));
        } else {
            logger.info("no vault secret and no POSTHOG_ENDPOINT, so posthog events go nowhere");
        }

        // Same rule as posthog: a process with no vault secret is a local one.
        var apiUrl = secrets.get("api.url", "API_URL", secrets.present() ? API_SERVER : LOCAL_API_SERVER);
        var maps = new MapClient.Http(new HttpClientWrapper(OpenTelemetry.noop(), apiUrl));

        var ipcUrl = secrets.get("ipc.url", "IPC_SERVICE_URL",
            secrets.present() ? IPC_SERVICE_URL : LOCAL_IPC_SERVICE_URL);
        var replays = new ReplayClient(HttpClient.newHttpClient(), ipcUrl);

        var slots = Integer.parseInt(secrets.get("worker.slots", "SLOTS", "4"));
        var worker = new Worker(db, hostName(), slots);
        worker.handle(JobSpec.PLAYER_COUNT, new PlayerCountRunner(db, PostHog.getClient()));
        worker.handle(JobSpec.INDEX_MAP, new IndexMapRunner(db, maps));
        worker.handle(JobSpec.COMPACT_REPLAY, new CompactReplayRunner(replays));
        worker.handle(JobSpec.RECONCILE_REPLAYS,
            new ReconcileReplaysRunner(db, Integer.parseInt(secrets.get("replay.batch", "REPLAY_BATCH", "1000"))));
        // SWEEP_REPLAY_SOURCES is deliberately not bound: nothing has been compacted to sweep yet,
        // and binding it is the switch. An unbound spec creates no row.

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Hand back what is running so the next replica picks it up now rather than when the
            // reaper notices.
            worker.close(STOP_GRACE);
            PostHog.shutdown();
            dataSource.close();
        }));
        worker.start();
    }

    /// The pod name in the cluster, so a picked row says which replica has it. Kubernetes sets
    /// `HOSTNAME`; a local run resolves it.
    private static String hostName() {
        var fromEnv = System.getenv("HOSTNAME");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "api-worker";
        }
    }

    private Main() {
    }
}
