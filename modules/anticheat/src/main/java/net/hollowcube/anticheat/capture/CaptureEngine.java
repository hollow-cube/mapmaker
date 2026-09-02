package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.*;
import net.hollowcube.anticheat.protocol.*;
import net.hollowcube.anticheat.state.StateCache;
import net.hollowcube.anticheat.state.TrackedEntity;
import net.hollowcube.anticheat.world.ChunkMap;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/// One connection's capture: the world model, the state cache, the ring, and the traces that come
/// out of them.
///
/// Everything the connection does happens on its event loop and nothing there blocks or touches a
/// disk. Frames are decoded when the registry has a decoder, applied to the model, appended to the
/// ring, and handed to a writer virtual thread through a queue; the writer owns the spool file and
/// every trace it assembles. What crosses between them is immutable — a [Snapshot], a [Frame],
/// a set of chunk positions — so neither side ever waits for the other.
///
/// The state machine is `IDLE → CAPTURING → CLOSING → IDLE`, where `CLOSING` is the handover in
/// [#stop] and lasts only as long as that call: the assembly itself happens on the writer, and a
/// new capture may start on top of it because the queue keeps the two in order.
public final class CaptureEngine implements FrameSink {

    private static final Logger logger = LoggerFactory.getLogger(CaptureEngine.class);

    /// Told about every trace that reaches the output directory, on the writer thread.
    @FunctionalInterface
    public interface Completion {

        void trace(Path path, TraceHeader header);
    }

    public enum Status {
        IDLE,
        CAPTURING,
        /// A capture is being handed to the writer; the connection is between captures.
        CLOSING
    }

    private final CaptureEngineConfig config;
    private final TraceHeader identity;
    private final Supplier<@Nullable String> clientBrand;
    private final CaptureClock clock;
    private final Completion completion;

    private final ChunkMap world = new ChunkMap();
    private final StateCache state = new StateCache();
    private final RingBuffer ring;
    private final Trim trim = new Trim();

    /// Caps the bytes of frame bodies waiting on the writer. [CaptureEngineConfig#queueSize()]
    /// bounds the count, which is the right bound for move-sized frames but would let a chunk
    /// burst against a stalled disk pin gigabytes of bodies.
    static final long MAX_PENDING_BYTES = 32L << 20;

    /// Unbounded so a stop or a shutdown is never lost; frames are bounded by [#pending] and
    /// [#pendingBytes] instead, which are the only thing that may be dropped.
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicLong pendingBytes = new AtomicLong();
    private final CountDownLatch writerDone = new CountDownLatch(1);

    private Status status = Status.IDLE;
    private long nowNs;
    private int currentPingId = Frame.NO_PING;
    private @Nullable Capture capture;
    private boolean closed;
    /// Written on the event loop, read by whoever samples metrics.
    private volatile long discardedCaptures;

    // Writer thread only, from here down.
    private @Nullable Path spoolPath;
    private @Nullable DataOutputStream spool;
    private long spoolBaseNs;
    /// The previous spooled frame's rebased time; frames are delta-encoded on the spool exactly
    /// as they are in the trace body.
    private long spoolLastTNs;
    private long spoolBytes;
    private boolean spoolTruncated;

    /// `clientBrand` is what the connection's owner knows of the client brand, asked whenever a
    /// trace header is built: the `minecraft:brand` payload the tap saw is preferred, but that
    /// payload can pass before the tap is even installed (see [#brand()]).
    public CaptureEngine(CaptureEngineConfig config, TraceHeader identity, Supplier<@Nullable String> clientBrand,
                         CaptureClock clock, Completion completion) {
        this(config, identity, clientBrand, clock, completion,
            task -> Thread.ofVirtual().name("anticheat-capture-writer").start(task));
    }

    @TestOnly
    CaptureEngine(CaptureEngineConfig config, TraceHeader identity, Supplier<@Nullable String> clientBrand,
                  CaptureClock clock, Completion completion, Executor writer) {
        this.config = config;
        this.identity = identity;
        this.clientBrand = clientBrand;
        this.clock = clock;
        this.completion = completion;
        this.ring = new RingBuffer(config.ringWindowNs(), config.ringSnapshotIntervalNs(), config.ringMaxBytes());
        writer.execute(this::run);
    }

    public Status status() {
        return status;
    }

    public @Nullable String captureId() {
        return capture == null ? null : capture.captureId;
    }

    public RingBuffer ring() {
        return ring;
    }

    /// Captures thrown away for being shorter than the floor, which is what the metric samples.
    public long discardedCaptures() {
        return discardedCaptures;
    }

    @Override
    public boolean frame(long tNs, Direction direction, ProtocolState protocolState, int packetId, int pingId, byte[] body) {
        if (closed) return false;
        var entry = Protocol776.lookup(protocolState, direction, packetId);
        if (!entry.kept()) return false;
        advance(tNs);
        if (pingId != Frame.NO_PING) currentPingId = pingId;

        // Before the frame is applied, so the snapshot and the frames kept after it line up.
        if (ring.wantsSnapshot(tNs)) {
            ring.snapshot(snapshot(tNs));
            trim.prune(earliestStartNs());
        }

        var packet = decode(entry, protocolState, direction, packetId, body);
        state.apply(protocolState, direction, packetId, body, packet);
        if (packet != null) {
            if (direction == Direction.S2C) world.handle(packet);
            note(tNs, packet);
        }

        boolean fence = entry.pingWhen() != null && packet != null
            && entry.pingWhen().fence(packet, state.entities().player().entityId());

        // A display entity is absent from the model, from the trim and from the prelude, so its
        // frames are the only place it leaks in — and on a display-heavy map they are most of the
        // trace. Decided after `state.apply`, so a promotion has already un-dropped its subject.
        if (packet instanceof EntityKeyed keyed && state.entities().isDropped(keyed.entityId())) return fence;

        var frameBody = packet instanceof S2CLevelChunkWithLight.V776 chunk
            ? sectionsOnly(chunk, body) : body;
        var frame = new Frame(tNs, direction, protocolState, packetId, pingId, frameBody);
        ring.frame(frame);

        var capture = this.capture;
        if (capture == null) return fence;
        capture.ping(pingId);
        append(frame);
        if (tNs - capture.snapshot.tNs() >= config.maxCaptureNs()) {
            capture.timeCapped = true;
            stop(TraceHeader.ClosedBy.STOP);
        }
        return fence;
    }

    /// Opens a capture: a snapshot now, a spool file, and every frame from here on written to both
    /// it and the ring. A second start closes the first as `superseded`.
    public void start(String captureId, TraceHeader.Reason reason, @Nullable TraceHeader.Cohort cohort,
                      TrimPolicy policy) {
        if (closed) return;
        advance(clock.nanoTime());
        if (capture != null) stop(TraceHeader.ClosedBy.SUPERSEDED);

        var snapshot = snapshot(nowNs);
        var spool = config.spoolDir().resolve(sanitize(captureId) + "-" + UUID.randomUUID() + ".spool");
        capture = new Capture(captureId, reason, cohort, policy, snapshot, clock.instant());
        status = Status.CAPTURING;
        // Wherever the player is standing when a capture opens is part of its region.
        var player = snapshot.player();
        if (player.entityId() >= 0) trim.add(nowNs, player.x(), player.z());
        queue.add(new Task.Open(spool, snapshot.tNs()));
    }

    /// Closes the active capture and hands it to the writer. A no-op when nothing is open.
    ///
    /// A capture shorter than [CaptureEngineConfig#minCaptureNs] is thrown away instead: it is the
    /// start snapshot and almost no frames, which costs the same to store as a real run and says
    /// nothing about how the player moved.
    public void stop(TraceHeader.ClosedBy closedBy) {
        var capture = this.capture;
        if (capture == null) return;
        advance(clock.nanoTime());
        this.capture = null;

        // Straight back to IDLE: nothing is handed to the writer to assemble, so there is no
        // CLOSING to pass through.
        if (nowNs - capture.snapshot.tNs() < config.minCaptureNs()) {
            discardedCaptures++;
            status = Status.IDLE;
            queue.add(Task.Discard.INSTANCE);
            trim.prune(earliestStartNs());
            return;
        }

        status = Status.CLOSING;
        var header = header(capture.captureId, capture.reason, capture.cohort, capture.policy, closedBy,
            capture.startedAt, clock.instant(), capture.pingIds(),
            flags(false, capture.timeCapped, tailUnfenced(closedBy)), capture.dropped);
        queue.add(new Task.Close(new Job(output(), header, capture.snapshot, capture.policy,
            trim.since(capture.snapshot.tNs()))));
        status = Status.IDLE;
        trim.prune(earliestStartNs());
    }

    /// Ships the ring: the oldest snapshot it kept and every frame since, without disturbing an
    /// active capture. A no-op before the first snapshot.
    public void flush(@Nullable String captureId, TraceHeader.Reason reason) {
        if (closed) return;
        var flush = ring.flush();
        if (flush == null) return;
        advance(clock.nanoTime());

        var now = clock.instant();
        var header = header(captureId, reason, null, config.trim(), TraceHeader.ClosedBy.FLUSH,
            now.minusNanos(nowNs - flush.snapshot().tNs()), now, pingIds(flush.frames()),
            flags(flush.truncated(), false, false), 0);
        queue.add(new Task.Flush(new Job(output(), header, flush.snapshot(), config.trim(),
            trim.since(flush.snapshot().tNs())), flush.frames()));
    }

    @Override
    public void disconnect(boolean shutdown) {
        stop(shutdown ? TraceHeader.ClosedBy.SHUTDOWN : TraceHeader.ClosedBy.DISCONNECT);
    }

    @Override
    public void close() {
        if (closed) return;
        stop(TraceHeader.ClosedBy.SHUTDOWN);
        closed = true;
        queue.add(Task.Shutdown.INSTANCE);
        try {
            if (!writerDone.await(config.closeTimeout().toNanos(), TimeUnit.NANOSECONDS))
                logger.warn("anticheat capture writer did not finish within {}", config.closeTimeout());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void advance(long tNs) {
        nowNs = Math.max(nowNs, tNs);
    }

    private Snapshot snapshot(long tNs) {
        return Snapshot.of(tNs, currentPingId, world, state);
    }

    /// The earliest point a trace could still start at, which is as far back as the trim has to
    /// remember.
    private long earliestStartNs() {
        long oldest = ring.oldestSnapshotNs();
        var capture = this.capture;
        return capture == null ? oldest : Math.min(oldest, capture.snapshot.tNs());
    }

    private @Nullable Packet decode(Protocol776.Entry entry, ProtocolState state, Direction direction, int packetId,
                                    byte[] body) {
        var decoder = entry.decoder();
        if (decoder == null) return null;
        try {
            return decoder.decode(new ByteReader(body));
        } catch (RuntimeException e) {
            // A packet we cannot read is still kept as bytes; only the model goes without it.
            logger.debug("anticheat capture failed to decode {} {} {} ({} bytes)", state, direction, packetId,
                body.length, e);
            return null;
        }
    }

    /// A chunk packet without its trailing block-entity and light blob, which measures 99% of one
    /// on a real connection (49KB of a 49.4KB packet, all of it uniform light) and which nothing
    /// A chunk packet cut down to the block sections, which are the only part of it anything above
    /// [S2CLevelChunkWithLight] reads. On a real map the heightmaps and the block-entity/light tail
    /// measured 11% and 99% of a packet respectively — storing them was what filled the ring with
    /// chunks on every join.
    ///
    /// Both are windows into `body`, so this splices rather than re-encodes: everything up to the
    /// heightmaps, an empty heightmap map in their place, then the section data up to where the
    /// tail starts. A decoder that handed back a slice of something else gets the body untouched.
    private static byte[] sectionsOnly(S2CLevelChunkWithLight.V776 chunk, byte[] body) {
        var heightmaps = chunk.heightmaps();
        var tail = chunk.blockEntitiesAndLight();
        if (heightmaps.array() != body || tail.array() != body) return body;

        int sections = heightmaps.offset() + heightmaps.length();
        var stripped = new byte[heightmaps.offset() + 1 + tail.offset() - sections];
        System.arraycopy(body, 0, stripped, 0, heightmaps.offset());
        stripped[heightmaps.offset()] = 0; // a varint zero: no heightmaps
        System.arraycopy(body, sections, stripped, heightmaps.offset() + 1, tail.offset() - sections);
        return stripped;
    }

    /// Notes what the trim region is built from: where the player is, and where an entity close
    /// enough to have touched them is.
    private void note(long tNs, Packet packet) {
        var player = state.entities().player();
        switch (packet) {
            case MovePlayer _, S2CPlayerPosition _ -> trim.add(tNs, player.x(), player.z());
            case S2CAddEntity entity -> note(tNs, entity.entityId(), player);
            case MoveEntity entity -> note(tNs, entity.entityId(), player);
            case S2CTeleportEntity entity -> note(tNs, entity.entityId(), player);
            case S2CEntityPositionSync entity -> note(tNs, entity.entityId(), player);
            default -> {
            }
        }
    }

    private void note(long tNs, int entityId, TrackedEntity player) {
        var entity = state.entities().get(entityId);
        if (entity == null || entity.dropped()) return;
        var capture = this.capture;
        double range = (capture == null ? config.trim() : capture.policy).entityRange();
        double dx = entity.x() - player.x();
        double dy = entity.y() - player.y();
        double dz = entity.z() - player.z();
        if (dx * dx + dy * dy + dz * dz > range * range) return;
        trim.add(tNs, entity.x(), entity.z());
    }

    /// Hands a frame to the writer, or counts it as dropped. The event loop never waits on the
    /// writer: a trace missing frames is worth more than a stalled connection.
    private void append(Frame frame) {
        var capture = this.capture;
        if (capture == null) return;
        if (pending.get() >= config.queueSize() || pendingBytes.get() >= MAX_PENDING_BYTES) {
            capture.dropped++;
            return;
        }
        pending.incrementAndGet();
        pendingBytes.addAndGet(frame.bytes().length);
        queue.add(new Task.Append(frame));
    }

    private Path output() {
        return config.outputDir().resolve(UUID.randomUUID() + ".trace");
    }

    private TraceHeader.Flags flags(boolean ringTruncated, boolean spoolTruncated, boolean tailUnfenced) {
        return new TraceHeader.Flags(ringTruncated, spoolTruncated, identity.flags().installedMidSession(),
            tailUnfenced);
    }

    /// Whether the connection ended with this trace, taking the final ping bracket's upper bound
    /// with it. Terminal frames (transfer, disconnect) are never fenced — see the packet table.
    private static boolean tailUnfenced(TraceHeader.ClosedBy closedBy) {
        return switch (closedBy) {
            case DISCONNECT, SHUTDOWN -> true;
            case STOP, SUPERSEDED, SWITCHED, FLUSH -> false;
        };
    }

    /// A trace header from the connection's identity plus what this capture knows. Everything the
    /// writer learns — counters, the spool cap — is filled in on the way out.
    private TraceHeader header(@Nullable String captureId, TraceHeader.Reason reason,
                                 @Nullable TraceHeader.Cohort cohort, TrimPolicy policy,
                                 TraceHeader.ClosedBy closedBy, Instant startedAt, Instant endedAt,
                                 @Nullable TraceHeader.PingIdRange pingIds, TraceHeader.Flags flags, long dropped) {
        return new TraceHeader(TraceFormat.VERSION_LATEST, TraceDictionary.LATEST, identity.clientPvn(), brand(),
            identity.playerId(), identity.playerName(),
            identity.connectionId(), captureId, reason, closedBy, cohort, policy.toHeader(),
            identity.proxy(), identity.proxyVersion(), startedAt, endedAt, pingIds, flags,
            new TraceHeader.Counters(0, 0, 0, 0, dropped), identity.extras());
    }

    /// The client brand a trace carries: the `minecraft:brand` payload this connection sent,
    /// falling back to whatever the proxy knows of it. The fallback is not a formality — the tap
    /// goes in at `PostLoginEvent`, which velocity fires asynchronously, so the client's brand
    /// payload has usually already passed by the time there is anything to see it.
    private @Nullable String brand() {
        var brand = state.brand();
        if (brand == null) brand = identity.brand();
        return brand == null ? clientBrand.get() : brand;
    }

    private static @Nullable TraceHeader.PingIdRange pingIds(List<Frame> frames) {
        int first = Frame.NO_PING;
        int last = Frame.NO_PING;
        for (var frame : frames) {
            if (frame.pingId() == Frame.NO_PING) continue;
            if (first == Frame.NO_PING) first = frame.pingId();
            last = frame.pingId();
        }
        return first == Frame.NO_PING ? null : new TraceHeader.PingIdRange(first, last);
    }

    private static String sanitize(String captureId) {
        var cleaned = captureId.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isEmpty() ? "capture" : cleaned;
    }

    /// What the tap said about a capture, and what it has cost so far. Event loop only.
    private static final class Capture {

        private final String captureId;
        private final TraceHeader.Reason reason;
        private final @Nullable TraceHeader.Cohort cohort;
        private final TrimPolicy policy;
        private final Snapshot snapshot;
        private final Instant startedAt;

        private int firstPingId = Frame.NO_PING;
        private int lastPingId = Frame.NO_PING;
        private long dropped;
        private boolean timeCapped;

        private Capture(String captureId, TraceHeader.Reason reason, @Nullable TraceHeader.Cohort cohort,
                        TrimPolicy policy, Snapshot snapshot, Instant startedAt) {
            this.captureId = captureId;
            this.reason = reason;
            this.cohort = cohort;
            this.policy = policy;
            this.snapshot = snapshot;
            this.startedAt = startedAt;
        }

        private void ping(int pingId) {
            if (pingId == Frame.NO_PING) return;
            if (firstPingId == Frame.NO_PING) firstPingId = pingId;
            lastPingId = pingId;
        }

        private @Nullable TraceHeader.PingIdRange pingIds() {
            return firstPingId == Frame.NO_PING
                ? null : new TraceHeader.PingIdRange(firstPingId, lastPingId);
        }
    }

    /// Everything the writer needs to turn a snapshot into a file. Immutable, and the only thing
    /// that crosses threads besides the frames themselves.
    private record Job(Path output, TraceHeader header, Snapshot snapshot, TrimPolicy policy, Set<Long> interest) {

        private Job {
            interest = Set.copyOf(interest);
        }
    }

    private sealed interface Task {

        record Open(Path spool, long baseNs) implements Task {
        }

        record Append(Frame frame) implements Task {
        }

        /// Close the spool and assemble the capture from it.
        record Close(Job job) implements Task {
        }

        /// Assemble a trace from frames the ring handed over; the spool is untouched.
        record Flush(Job job, List<Frame> frames) implements Task {
        }

        /// Close and delete the spool without assembling anything.
        record Discard() implements Task {

            public static final Discard INSTANCE = new Discard();
        }

        record Shutdown() implements Task {

            public static final Shutdown INSTANCE = new Shutdown();
        }
    }

    // Everything below runs on the writer thread.

    private void run() {
        try {
            while (true) {
                var task = queue.take();
                switch (task) {
                    case Task.Open open -> open(open);
                    case Task.Append(Frame frame) -> {
                        try {
                            spool(frame);
                        } finally {
                            pending.decrementAndGet();
                            pendingBytes.addAndGet(-frame.bytes().length);
                        }
                    }
                    case Task.Close(Job job) -> close(job);
                    case Task.Discard _ -> discardSpool();
                    case Task.Flush(Job job, List<Frame> frames) -> flush(job, frames);
                    case Task.Shutdown _ -> {
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            discardSpool();
            writerDone.countDown();
        }
    }

    private void open(Task.Open open) {
        discardSpool();
        spoolBaseNs = open.baseNs();
        spoolLastTNs = 0;
        spoolBytes = 0;
        spoolTruncated = false;
        try {
            Files.createDirectories(open.spool().getParent());
            spool = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(open.spool()), 1 << 16));
            spoolPath = open.spool();
        } catch (IOException e) {
            logger.warn("anticheat capture could not open spool {}", open.spool(), e);
            spool = null;
            spoolPath = null;
            spoolTruncated = true;
        }
    }

    private void spool(Frame frame) {
        var out = spool;
        if (out == null || spoolTruncated) return;
        if (spoolBytes + frame.bytes().length > config.maxSpoolBytes()) {
            // The cap ends the spool rather than filtering it: the frames that would still fit are
            // the zero-byte tick ends, which would record a client that keeps ticking but never
            // moves.
            spoolTruncated = true;
            return;
        }
        try {
            var rebased = rebase(frame, spoolBaseNs);
            rebased.encode(out, spoolLastTNs);
            spoolLastTNs = rebased.tNs();
            spoolBytes += frame.bytes().length;
        } catch (IOException e) {
            logger.warn("anticheat capture could not spool a frame", e);
            spoolTruncated = true;
            spool = null;
        }
    }

    private void close(Job job) {
        var path = spoolPath;
        boolean truncated = spoolTruncated;
        closeSpool();
        spoolPath = null;
        try {
            assemble(job, path == null ? FrameSource.EMPTY : spoolSource(path), truncated);
        } finally {
            if (path != null) delete(path);
        }
    }

    private void flush(Job job, List<Frame> frames) {
        long base = job.snapshot().tNs();
        assemble(job, sink -> {
            for (var frame : frames) sink.accept(rebase(frame, base));
        }, false);
    }

    private void assemble(Job job, FrameSource frames, boolean spoolTruncated) {
        var output = job.output();
        var temp = output.resolveSibling(output.getFileName() + ".tmp");
        try {
            Files.createDirectories(output.getParent());
            var header = spoolTruncated
                ? job.header().withFlags(job.header().flags().withSpoolTruncated(true))
                : job.header();
            var prelude = Prelude.frames(job.snapshot());
            var world = Trim.world(job.snapshot().world(), job.policy(), job.interest());

            var written = TraceWriter.assemble(temp, header, prelude, world, frames);
            Files.move(temp, output, StandardCopyOption.ATOMIC_MOVE);
            completion.trace(output, written);
        } catch (IOException | RuntimeException e) {
            logger.warn("anticheat capture failed to assemble {}", output, e);
            delete(temp);
        }
    }

    /// The spool read back, frame by frame. A file cut short by a crash or the cap stops where it
    /// stops: what is there is still exactly what the connection carried.
    private static FrameSource spoolSource(Path path) {
        return sink -> {
            try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path), 1 << 16))) {
                long lastTNs = 0;
                while (true) {
                    var frame = Frame.decode(in, lastTNs);
                    lastTNs = frame.tNs();
                    sink.accept(frame);
                }
            } catch (EOFException end) {
                // The end of the spool, which is the only way it ends.
            } catch (IOException e) {
                throw new UncheckedIOException("failed to read capture spool " + path, e);
            }
        };
    }

    private void closeSpool() {
        var out = spool;
        spool = null;
        if (out == null) return;
        try {
            out.close();
        } catch (IOException e) {
            logger.warn("anticheat capture could not close its spool", e);
        }
    }

    private void discardSpool() {
        var path = spoolPath;
        closeSpool();
        spoolPath = null;
        if (path != null) delete(path);
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("anticheat capture could not delete {}", path, e);
        }
    }

    private static Frame rebase(Frame frame, long baseNs) {
        return new Frame(frame.tNs() - baseNs, frame.direction(), frame.state(), frame.packetId(), frame.pingId(),
            frame.bytes());
    }
}
