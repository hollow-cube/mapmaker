package net.hollowcube.proxy.anticheat;

import net.hollowcube.anticheat.capture.CaptureEngine;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.anticheat.log.TraceReader;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.anticheat.AnticheatClient;
import net.hollowcube.ipc.anticheat.TraceMeta;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/// Gets finished traces off the proxy and into the store: [AnticheatClient#putTrace] with the
/// trace's header cut down to the [TraceMeta] a row is found by, so the store files an upload
/// without decompressing it.
///
/// One virtual thread per trace, since a ship is a large body over a network and nothing else
/// waits on it. A 4xx is never retried — retrying a refusal is how a spool fills up — and anything
/// else is, with a doubling backoff, for as long as [#RETRY_WINDOW]. The generated client has no
/// per-request timeout and the connect timeout covers setup alone, so a ship that hangs mid-body is
/// bounded by nothing but the interrupt [#close] hands it at shutdown.
///
/// The sweeper is the other half, and the reason a trace is never held hostage by the one in
/// front of it: it enforces the total spool cap and re-ships whatever is still lying around, which
/// is how a trace a previous proxy process never got to is picked up at startup.
public final class TraceShipper implements CaptureEngine.Completion, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TraceShipper.class);

    /// The store's own rule for an id, checked here so a file that could never be stored is not
    /// shipped ten times to find that out.
    private static final Pattern TRACE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final String EXTENSION = ".trace";

    private static final Duration MAX_BACKOFF = Duration.ofSeconds(60);
    /// The plan's ten minutes: long enough to outlast a store restart, short enough that a trace
    /// is not held hostage by one that will never land.
    public static final Duration RETRY_WINDOW = Duration.ofMinutes(10);
    public static final Duration BASE_BACKOFF = Duration.ofSeconds(2);
    public static final Duration SWEEP_INTERVAL = Duration.ofSeconds(60);

    /// What one ship came to. Everything but [Retry] ends the trace's life on this proxy.
    private sealed interface Outcome {

        record Stored(long bytes) implements Outcome {
        }

        /// The store refused the trace outright; sending it again would be refused again.
        record Refused(int status) implements Outcome {
        }

        /// The file is not there any more, which is the sweeper's cap or a second ship of it.
        record Gone(String why) implements Outcome {
        }

        record Retry(String cause) implements Outcome {
        }
    }

    private final AnticheatConfig config;

    private final Duration retryWindow;
    private final Duration baseBackoff;
    private final Duration sweepInterval;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AnticheatClient store;
    private final ExecutorService ships =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("anticheat-ship-", 0).factory());
    private final ScheduledExecutorService sweeps = Executors.newSingleThreadScheduledExecutor(
        task -> Thread.ofPlatform().daemon().name("anticheat-sweeper").unstarted(task));

    /// The traces a virtual thread is already looking after, so a sweep never ships or deletes
    /// one out from under a ship in progress.
    private final Set<Path> inFlight = ConcurrentHashMap.newKeySet();
    private volatile boolean running = true;

    /// `ipcUrl` is the same root every other ipc client is built on (`IPC_SERVICE_URL`); traces
    /// are just another service under it.
    public TraceShipper(String ipcUrl, AnticheatConfig config) {
        this(ipcUrl, config, RETRY_WINDOW, BASE_BACKOFF, SWEEP_INTERVAL);
    }

    @TestOnly
    TraceShipper(String ipcUrl, AnticheatConfig config, Duration retryWindow, Duration baseBackoff,
                 Duration sweepInterval) {
        this.config = config;
        this.retryWindow = retryWindow;
        this.baseBackoff = baseBackoff;
        this.sweepInterval = sweepInterval;
        this.store = new AnticheatClient(http, ipcUrl);
    }

    /// Starts sweeping, beginning with what a previous run of this proxy left behind.
    public void start() {
        sweeps.scheduleWithFixedDelay(() -> ships.execute(this::sweep), 0, sweepInterval.toNanos(),
            TimeUnit.NANOSECONDS);
    }

    /// A capture engine finished a trace, on its writer thread. The header it hands over is the
    /// one that landed on disk, so a ship straight off the engine never reads the file back.
    @Override
    public void trace(Path path, TraceHeader header) {
        ship(path, header);
    }

    /// Ships `path`, reading its header off disk when the caller has none (the sweeper, or a
    /// trace left by a previous process). Doing nothing when it is already being shipped.
    public void ship(Path path, @Nullable TraceHeader header) {
        if (!running || !inFlight.add(path)) return;
        try {
            ships.execute(() -> {
                try {
                    deliver(path, header);
                } finally {
                    inFlight.remove(path);
                }
            });
        } catch (RuntimeException e) {
            inFlight.remove(path);
            logger.warn("anticheat: could not start shipping {}", path, e);
        }
    }

    /// Stops sweeping and gives whatever is in flight `grace` to finish; a ship still going after
    /// that is interrupted and its trace left in the spool for the next process to sweep up.
    public void close(Duration grace) {
        if (!running) return;
        running = false;
        sweeps.shutdownNow();
        ships.shutdown();
        try {
            if (!ships.awaitTermination(grace.toNanos(), TimeUnit.NANOSECONDS)) {
                logger.warn("anticheat: {} traces still shipping after {}, leaving them spooled", inFlight.size(),
                    grace);
                ships.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ships.shutdownNow();
        }
        http.shutdownNow();
    }

    @Override
    public void close() {
        close(config.shutdownGrace());
    }

    private void deliver(Path path, @Nullable TraceHeader known) {
        var id = traceId(path);
        if (id == null) {
            refused(path, null, null, "it is not a trace name the store would take");
            return;
        }

        TraceHeader header;
        try {
            header = known != null ? known : TraceReader.header(path);
        } catch (RuntimeException e) {
            if (!Files.exists(path)) return; // Swept out from under us, which is not a bad trace.
            logger.warn("anticheat: cannot read the header of {}, not shipping it", path, e);
            refused(path, id, null, "its header cannot be read");
            return;
        }

        var meta = meta(id, path, header);
        if (meta == null) {
            refused(path, id, header, "it names no player, so the store has no row to file it under");
            return;
        }

        var started = System.nanoTime();
        var deadline = started + retryWindow.toNanos();
        var backoff = baseBackoff;
        while (true) {
            switch (put(path, meta)) {
                case Outcome.Stored(long bytes) -> {
                    stored(path, id, header, bytes, started);
                    return;
                }
                case Outcome.Refused(int status) -> {
                    refused(path, id, header, status);
                    return;
                }
                case Outcome.Gone(String why) -> {
                    logger.debug("anticheat: trace {} is gone ({})", id, why);
                    return;
                }
                case Outcome.Retry(String cause) -> {
                    if (!running || System.nanoTime() + backoff.toNanos() > deadline) {
                        deferred(id, cause);
                        return;
                    }
                    logger.debug("anticheat: retrying trace {} in {} ({})", id, backoff, cause);
                    if (!sleep(backoff)) {
                        deferred(id, cause);
                        return;
                    }
                    backoff = min(backoff.multipliedBy(2), MAX_BACKOFF);
                }
            }
        }
    }

    private Outcome put(Path path, TraceMeta meta) {
        try (var body = Blob.of(path)) {
            return new Outcome.Stored(store.putTrace(meta, body).bytes());
        } catch (NoSuchFileException e) {
            return new Outcome.Gone(String.valueOf(e.getMessage()));
        } catch (IpcException e) {
            // 400 is a meta the store cannot index and 413 a trace it will not hold; both stay
            // wrong however many times they are sent. Everything else — a 5xx, or the zero of a
            // call that never got an answer — is worth another go.
            if (e.status() >= 400 && e.status() < 500) return new Outcome.Refused(e.status());
            return new Outcome.Retry(e.getMessage());
        } catch (IOException | RuntimeException e) {
            return new Outcome.Retry(String.valueOf(e));
        }
    }

    /// The header cut to what the store files a row by, and null when it names no player: such a
    /// trace has no row to go in, which is a refusal wherever it is worked out. A header written
    /// without a start time falls back to the file's own mtime, since a row stamped 0 would sort
    /// before every real trace.
    private static @Nullable TraceMeta meta(String id, Path path, TraceHeader header) {
        var playerId = header.playerId();
        if (playerId == null) return null;
        return new TraceMeta(id, header.captureId(), playerId.toString(), header.proxyVersion(),
            header.proxy(), header.clientPvn(), reason(header.reason()),
            header.startedAt() == null ? modifiedAt(path) : header.startedAt().toEpochMilli(),
            header.endedAt() == null ? null : header.endedAt().toEpochMilli(),
            header.formatVersion());
    }

    /// The reason as the store's table spells it, off the header's own gson rather than off the
    /// constant's name: the store answers 400 for a word its table does not have, and a 400 is a
    /// refusal this never retries.
    private static @Nullable String reason(TraceHeader.@Nullable Reason reason) {
        return reason == null ? null : TraceHeader.GSON.toJsonTree(reason).getAsString();
    }

    private void stored(Path path, String id, TraceHeader header, long bytes,
                        long startedNs) {
        AnticheatMetrics.shipDuration.observe((System.nanoTime() - startedNs) / 1e9);
        AnticheatMetrics.traces.labels(String.valueOf(header.reason()), String.valueOf(header.closedBy()), "shipped")
            .inc();
        delete(path);
        logger.info("anticheat: shipped trace {} ({} bytes) for {}", id, bytes, header.playerName());
    }

    private void refused(Path path, @Nullable String id, @Nullable TraceHeader header, int status) {
        refused(path, id, header, "the store refused it with " + status);
    }

    /// A trace that will never be stored, kept out of the sweeper's way rather than deleted: it
    /// is the only copy, and `why` is worth a look. Refusals worked out here rather than by the
    /// store have no status to name, hence the reason rather than a made-up 0.
    private void refused(Path path, @Nullable String id, @Nullable TraceHeader header, String why) {
        AnticheatMetrics.traces.labels(header == null ? "null" : String.valueOf(header.reason()),
            header == null ? "null" : String.valueOf(header.closedBy()), "refused").inc();
        try {
            Files.createDirectories(config.failedDir());
            Files.move(path, config.failedDir().resolve(path.getFileName().toString()),
                StandardCopyOption.REPLACE_EXISTING);
            logger.warn("anticheat: trace {} was refused ({}), kept in {}", id, why, config.failedDir());
        } catch (IOException e) {
            logger.warn("anticheat: could not put the refused trace {} aside", path, e);
        }
    }

    private void deferred(String id, String cause) {
        AnticheatMetrics.traces.labels("null", "null", "deferred").inc();
        logger.warn("anticheat: giving up on trace {} for now ({}), left for the sweeper", id, cause);
    }

    /// One pass over the spool: the cap first, so nothing is uploaded on its way to being deleted,
    /// then everything still there.
    private void sweep() {
        try {
            var traces = traces();
            enforceCap(traces);
            for (var path : traces) {
                if (Files.exists(path)) ship(path, null);
            }
        } catch (RuntimeException e) {
            logger.warn("anticheat: spool sweep failed", e);
        }
    }

    /// The plan's total spool cap, over the whole spool directory: oldest traces go first, and a
    /// trace being shipped right now is not one of them.
    private void enforceCap(List<Path> traces) {
        var total = spoolBytes();
        for (var path : traces) {
            if (total <= config.spoolMaxBytes()) break;
            if (inFlight.contains(path)) continue;
            var bytes = size(path);
            if (!delete(path)) continue;
            total -= bytes;
            AnticheatMetrics.dropped(AnticheatMetrics.Drop.SPOOL_CAP);
            logger.warn("anticheat: spool over {} bytes, dropped trace {}", config.spoolMaxBytes(),
                path.getFileName());
        }
        AnticheatMetrics.spoolBytes.set(total);
    }

    /// Every trace waiting in the spool, oldest first, which is the order the cap eats them in.
    private List<Path> traces() {
        try (var files = Files.list(config.tracesDir())) {
            return files.filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparingLong(TraceShipper::modifiedAt))
                .toList();
        } catch (NoSuchFileException e) {
            return List.of(); // Nothing has been captured yet.
        } catch (IOException e) {
            logger.warn("anticheat: cannot list {}", config.tracesDir(), e);
            return List.of();
        }
    }

    /// Everything on the spool volume, open captures included, since the cap is about the disk the
    /// proxy was given rather than about traces alone.
    private long spoolBytes() {
        try (var tree = Files.walk(config.spoolDir())) {
            return tree.filter(Files::isRegularFile).mapToLong(TraceShipper::size).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private boolean delete(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            logger.warn("anticheat: could not delete {}", path, e);
            return false;
        }
    }

    private boolean sleep(Duration duration) {
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /// The file name without its extension, or null when the store would refuse the id anyway.
    static @Nullable String traceId(Path path) {
        var name = path.getFileName().toString();
        var id = name.endsWith(EXTENSION) ? name.substring(0, name.length() - EXTENSION.length()) : name;
        return TRACE_ID.matcher(id).matches() ? id : null;
    }

    private static long modifiedAt(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE; // Unreadable is not something to delete first.
        }
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private static Duration min(Duration left, Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }
}
