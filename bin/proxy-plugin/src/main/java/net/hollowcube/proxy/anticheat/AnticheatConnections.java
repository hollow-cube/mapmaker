package net.hollowcube.proxy.anticheat;

import io.netty.channel.EventLoop;
import net.hollowcube.anticheat.Protocol;
import net.hollowcube.anticheat.capture.CaptureClock;
import net.hollowcube.anticheat.capture.CaptureEngine;
import net.hollowcube.anticheat.capture.CaptureEngineConfig;
import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.control.CaptureControl;
import net.hollowcube.anticheat.log.TraceFormat;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.anticheat.protocol.ProtocolState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/// Puts a tap and a capture engine on every connection worth capturing, and takes them off again.
///
/// Installation happens at `PostLoginEvent`, the first point where the channel exists and the
/// client's protocol version is settled; the connection is in the configuration phase by then, both
/// ways, because velocity only fires the event once the client has acknowledged the login.
/// Anything but 776 is left alone and counted — there is no packet table for it yet.
///
/// Captures are opened and closed from the backend, over the `mapmaker:anticheat` channel
/// ([#handleBackend]); the engine is owned by the connection's event loop, so [#start], [#stop]
/// and [#flush] only ever hand it a task to run there. Finished traces leave through [#trace],
/// which is where the shipper hangs off.
public final class AnticheatConnections {

    private static final Logger logger = LoggerFactory.getLogger(AnticheatConnections.class);

    /// Missing on a control message the backend really did send, rather than absent on purpose.
    private static final TraceHeader.Reason DEFAULT_REASON = TraceHeader.Reason.MANUAL;

    private final AnticheatConfig config;
    private final CaptureClock clock;
    private final String proxy;
    private final String proxyVersion;
    private final BooleanSupplier shuttingDown;
    private final CaptureEngine.Completion ship;

    private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

    /// `shuttingDown` is what every tap asks when its channel goes inactive, so a capture the
    /// proxy's own shutdown ended is closed as such rather than as a player leaving. `ship` is told
    /// about every trace that reaches the output directory, on the engine's writer thread.
    public AnticheatConnections(AnticheatConfig config, CaptureClock clock, String proxy,
                                String proxyVersion, BooleanSupplier shuttingDown,
                                CaptureEngine.Completion ship) {
        this.config = config;
        this.clock = clock;
        this.proxy = proxy;
        this.proxyVersion = proxyVersion;
        this.shuttingDown = shuttingDown;
        this.ship = ship;
    }

    /// One connection the proxy is holding, either side of the only question anybody asks about
    /// it: is there an engine on it or not.
    private sealed interface Connection {

        /// The client protocol version, which is a gauge label rather than anything behavioural.
        String pvn();

        boolean tapped();

        /// `capturing` is the `anticheat_captures_active` gauge's share of this connection, and is
        /// set on the event loop by [AnticheatConnections#start] and [AnticheatConnections#stop]
        /// and cleared by [AnticheatConnections#quit] — a capture the engine ends by itself (the
        /// ten minute cap) leaves it set until the player goes.
        /// `ringDropsSeen` is the eviction count [AnticheatConnections#sample()] last charged to
        /// `anticheat_dropped_total`, kept per connection so a connection leaving cannot make the
        /// total go backwards.
        record Tapped(CaptureEngine engine, EventLoop eventLoop, AtomicBoolean capturing,
                      AtomicLong ringDropsSeen, String pvn) implements Connection {

            @Override
            public boolean tapped() {
                return true;
            }
        }

        record Untapped(String pvn) implements Connection {

            @Override
            public boolean tapped() {
                return false;
            }
        }
    }

    /// The player is past login. `player` is a velocity `ConnectedPlayer`, taken as an `Object` so
    /// this is testable without a proxy (see [VelocityInternals]); `clientBrand` is that same
    /// player's `Player#getClientBrand()`, which is the only brand a trace gets when the client's
    /// `minecraft:brand` payload beat the tap onto the pipeline — the usual case, because velocity
    /// fires `PostLoginEvent` off the event loop and the configuration phase does not wait for it.
    ///
    /// Returns the tap that went in, or null when the connection was left alone.
    public @Nullable AnticheatTap join(Object player, UUID playerId, String playerName,
                                       int protocolVersion, Supplier<@Nullable String> clientBrand) {
        if (!config.enabled()) return null;

        var pvn = Integer.toString(protocolVersion);
        if (!Protocol.isSupported(protocolVersion)) {
            AnticheatMetrics.connections.labels(pvn, "false").inc();
            AnticheatMetrics.dropped(AnticheatMetrics.Drop.UNSUPPORTED_PVN);
            connections.put(playerId, new Connection.Untapped(pvn));
            return null;
        }

        var channel = VelocityInternals.channelOf(player);
        if (channel == null) {
            // The same bucket as a version with no table: either way the tap is not installed.
            logger.error("anticheat: no channel behind {} ({}), not tapping", playerName, player.getClass().getName());
            AnticheatMetrics.connections.labels(pvn, "false").inc();
            AnticheatMetrics.dropped(AnticheatMetrics.Drop.UNSUPPORTED_PVN);
            connections.put(playerId, new Connection.Untapped(pvn));
            return null;
        }

        var connectionId = UUID.randomUUID().toString();
        var engine = new CaptureEngine(engineConfig(connectionId),
            identity(protocolVersion, playerId, playerName, connectionId), clientBrand, clock, this::trace);
        // PostLoginEvent fires on the login acknowledgement, so both directions are in configuration.
        var tap = new AnticheatTap(engine, clock, shuttingDown,
            ProtocolState.CONFIGURATION, ProtocolState.CONFIGURATION);
        if (!tap.install(channel.pipeline())) {
            engine.close();
            AnticheatMetrics.connections.labels(pvn, "false").inc();
            connections.put(playerId, new Connection.Untapped(pvn));
            return null;
        }

        AnticheatMetrics.connections.labels(pvn, "true").inc();
        connections.put(playerId, new Connection.Tapped(engine, channel.eventLoop(), new AtomicBoolean(),
            new AtomicLong(), pvn));
        logger.debug("anticheat: tapped {} ({}), connection {}", playerName, pvn, connectionId);
        return tap;
    }

    /// The player is gone. The channel is already closed by now, so the tap has told the engine;
    /// this closes it out, waiting the configured grace for anything still being written.
    public void quit(UUID playerId) {
        var connection = connections.remove(playerId);
        if (connection == null) return;
        AnticheatMetrics.connections.labels(connection.pvn(), Boolean.toString(connection.tapped())).dec();
        if (!(connection instanceof Connection.Tapped tapped)) return;
        if (tapped.capturing().compareAndSet(true, false)) AnticheatMetrics.capturesActive.dec();
        tapped.engine().close();
    }

    /// A control message from the player's current backend server, the only source that may open a
    /// capture. A message that does not parse or says nothing actionable is a bug on one side of
    /// the deploy boundary, worth a real log line and nothing else.
    public void handleBackend(UUID playerId, String playerName, byte[] data) {
        CaptureControl message;
        try {
            message = CaptureControl.decode(data);
        } catch (RuntimeException e) {
            logger.warn("anticheat: unreadable control message for {} ({} bytes)", playerName, data.length, e);
            return;
        }

        var dispatched = switch (message) {
            case CaptureControl.Start(var captureId, var reason, var cohort, var trim) -> captureId == null
                ? warn(playerName, "start without a capture id")
                : start(playerId, captureId, Objects.requireNonNullElse(reason, DEFAULT_REASON), cohort,
                    Objects.requireNonNullElse(trim, TrimPolicy.DEFAULT));
            case CaptureControl.Stop(var captureId) -> captureId == null
                ? warn(playerName, "stop without a capture id")
                : stop(playerId, captureId);
            case CaptureControl.Flush(var captureId, var reason) ->
                flush(playerId, captureId, Objects.requireNonNullElse(reason, DEFAULT_REASON));
        };
        if (!dispatched) logger.debug("anticheat: no tapped connection for {}, dropping {}", playerName, message);
    }

    /// True so the caller counts it as dispatched: the warn is the whole story, not a second
    /// "no tapped connection" line on top.
    private boolean warn(String playerName, String what) {
        logger.warn("anticheat: dropped {} for {}", what, playerName);
        return true;
    }

    /// Opens a capture, on the connection's event loop. False when nothing is tapped for the player.
    public boolean start(UUID playerId, String captureId, TraceHeader.Reason reason,
                         @Nullable TraceHeader.Cohort cohort, TrimPolicy trim) {
        return onEngine(playerId, (connection, engine) -> {
            engine.start(captureId, reason, cohort, trim);
            if (engine.status() == CaptureEngine.Status.CAPTURING && connection.capturing().compareAndSet(false, true))
                AnticheatMetrics.capturesActive.inc();
        });
    }

    /// Closes the capture with this id, if it is the one that connection has open.
    public boolean stop(UUID playerId, String captureId) {
        return onEngine(playerId, (connection, engine) -> {
            if (!captureId.equals(engine.captureId())) return;
            engine.stop(TraceHeader.ClosedBy.STOP);
            if (connection.capturing().compareAndSet(true, false)) AnticheatMetrics.capturesActive.dec();
        });
    }

    /// Ships the ring, which does not disturb whatever capture is open.
    public boolean flush(UUID playerId, @Nullable String captureId, TraceHeader.Reason reason) {
        return onEngine(playerId, (connection, engine) -> engine.flush(captureId, reason));
    }

    /// Every bit of engine state is owned by the connection's netty event loop, so anything the
    /// control channel asks for runs there rather than on the thread velocity delivered it on.
    private boolean onEngine(UUID playerId, BiConsumer<Connection.Tapped, CaptureEngine> action) {
        if (!(connections.get(playerId) instanceof Connection.Tapped tapped)) return false;
        tapped.eventLoop().execute(() -> action.accept(tapped, tapped.engine()));
        return true;
    }

    /// The metrics only the connections know: the ring bytes they are holding, and the frames their
    /// ring caps have cost since the last look. Sampled on a timer rather than published per frame,
    /// because the event loop has better things to do than maintain a gauge.
    public void sample() {
        long bytes = 0;
        for (var connection : connections.values()) {
            if (!(connection instanceof Connection.Tapped tapped)) continue;
            var ring = tapped.engine().ring();
            bytes += ring.bytes();
            long evicted = ring.evictedFrames();
            long seen = tapped.ringDropsSeen().getAndSet(evicted);
            if (evicted > seen) AnticheatMetrics.dropped(AnticheatMetrics.Drop.RING_CAP, evicted - seen);
        }
        AnticheatMetrics.ringBytes.set(bytes);
    }

    /// Proxy shutdown: every open engine closed out, with the same grace each. The traces that
    /// come of it are shipped like any other, so what is left is the shipper's grace, not this one.
    public void close() {
        for (var playerId : Map.copyOf(connections).keySet()) quit(playerId);
    }

    /// The one place a finished trace arrives, on the engine's writer thread: counted here and
    /// handed to the shipper, which owns it (and the spool file) from now on.
    private void trace(Path path, TraceHeader header) {
        AnticheatMetrics.traces.labels(String.valueOf(header.reason()), String.valueOf(header.closedBy()), "spooled").inc();
        AnticheatMetrics.traceBytes.observe(header.counters().bytes());
        logger.info("anticheat: trace {} ({} frames, {} bytes) awaiting shipping", path,
            header.counters().frames(), header.counters().bytes());
        ship.trace(path, header);
    }

    private CaptureEngineConfig engineConfig(String connectionId) {
        var spool = config.spoolDir().resolve(connectionId);
        // The plan's spool cap is over the whole directory; per connection is what the engine can
        // enforce, and the shipper's sweeper owns the total.
        return new CaptureEngineConfig(spool, config.tracesDir(),
            config.ringWindow().toNanos(), CaptureEngineConfig.RING_SNAPSHOT_INTERVAL_NS,
            config.ringMaxBytes(), config.spoolMaxBytes(), CaptureEngineConfig.MAX_CAPTURE_NS,
            CaptureEngineConfig.QUEUE_SIZE, TrimPolicy.DEFAULT, config.shutdownGrace());
    }

    /// The connection fields every trace of this connection carries. The session service keys a
    /// session by the player's uuid, so that is the session id.
    private TraceHeader identity(int protocolVersion, UUID playerId, String playerName,
                                   String connectionId) {
        return new TraceHeader(TraceFormat.VERSION_LATEST, protocolVersion, null, playerId, playerName,
            connectionId, null, null, null, null, null, proxy, proxyVersion, null, null, null,
            TraceHeader.Flags.NONE, TraceHeader.Counters.EMPTY, Map.of());
    }
}
