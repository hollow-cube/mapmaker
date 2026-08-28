package net.hollowcube.apiworker;

import net.hollowcube.apiserver.common.Pools;
import net.hollowcube.apiserver.common.PostHogIds;
import net.hollowcube.apiserver.common.PostgresUri;
import net.hollowcube.apiserver.common.VaultSecrets;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiworker.job.Worker;
import net.hollowcube.apiworker.jobs.PlayerCountRunner;
import net.hollowcube.posthog.PostHog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public static void main(String[] args) {
        var secrets = VaultSecrets.load();
        var dataSource = Pools.postgres(PostgresUri.parse(secrets.require("postgres.maps_uri", "DATABASE_URL")), "api-worker");
        var db = new ApiDatabase(dataSource);

        // The uninitialised client drops everything, which is what a local run wants: the Go
        // server likewise sends nothing under tilt.
        var endpoint = secrets.get("posthog.endpoint", "POSTHOG_ENDPOINT");
        if (endpoint != null) PostHog.init(PostHogIds.PROJECT_KEY, config -> config.endpoint(endpoint));
        else logger.info("posthog.endpoint is not set, events go nowhere");

        var slots = Integer.parseInt(secrets.get("worker.slots", "SLOTS", "4"));
        var worker = new Worker(db, hostName(), slots);
        worker.handle(JobSpec.PLAYER_COUNT, new PlayerCountRunner(db, PostHog.getClient()));

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
