package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.Frame;
import net.hollowcube.anticheat.log.TraceFormat;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.anticheat.protocol.*;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The pieces the capture tests share: a clock they step by hand, a completion callback they can
/// wait on, and hand-built 776 packets to feed an engine with.
final class TestCapture {

    static final long SECOND = 1_000_000_000L;
    static final int PLAYER_ID = 7;

    private TestCapture() {
    }

    static CaptureEngineConfig config(Path directory) {
        return new CaptureEngineConfig(directory.resolve("spool"), directory.resolve("out"),
            60 * SECOND, 30 * SECOND, 64L << 20, 64L << 20, 600 * SECOND, 0, 1 << 20,
            TrimPolicy.DEFAULT, Duration.ofSeconds(5));
    }

    /// The connection fields every trace of one connection carries.
    static TraceHeader identity() {
        return new TraceHeader(TraceFormat.VERSION_LATEST, Protocol776.PROTOCOL_VERSION, null,
            UUID.fromString("00000000-0000-0000-0000-0000000000aa"), "Tester", "connection-1",
            null, null, null, null, null, "proxy-1", "test", null, null, null,
            TraceHeader.Flags.NONE, TraceHeader.Counters.EMPTY, Map.of());
    }

    static boolean feed(CaptureEngine engine, long tNs, ProtocolState state, Direction direction, String name, Packet packet) {
        return engine.frame(tNs, direction, state, Protocol776.packetId(state, direction, name), Frame.NO_PING,
            packet.toByteArray());
    }

    static void move(CaptureEngine engine, long tNs, double x, double y, double z) {
        feed(engine, tNs, ProtocolState.PLAY, Direction.C2S, "move_player_pos", new C2SMovePlayerPos.V776(x, y, z, 1));
    }

    static S2CLogin.V776 login() {
        return new S2CLogin.V776(PLAYER_ID, false, List.of("minecraft:overworld"), 20, 32, 12, false, true, false,
            new CommonPlayerSpawnInfo(0, "minecraft:overworld", 0L, 0, (byte) -1, false, false, null, 0, 63),
            false, false);
    }

    static S2CSetChunkCacheRadius.V776 viewDistance(int radius) {
        return new S2CSetChunkCacheRadius.V776(radius);
    }

    /// `entityTypeId` is a 776 id: 132 is a text display, 69 a pig.
    static S2CAddEntity.V776 addEntity(int entityId, int entityTypeId) {
        return new S2CAddEntity.V776(entityId, new UUID(0, entityId), entityTypeId, 8, 64, 8,
            LpVec3.ZERO, (byte) 0, (byte) 0, (byte) 0, 0);
    }

    static S2CEntityPositionSync.V776 positionSync(int entityId) {
        return new S2CEntityPositionSync.V776(entityId,
            new PositionMoveRotation(8, 64, 8, 0, 0, 0, 0, 0), true);
    }

    /// A chunk of single-value air sections, the smallest legal shape.
    static S2CLevelChunkWithLight.V776 chunk(int chunkX, int chunkZ) {
        var sections = new ArrayList<Section>(4);
        for (int i = 0; i < 4; i++) sections.add(new Section(0, 0, 0, new int[]{0}, new long[0], biomes()));
        return new S2CLevelChunkWithLight.V776(chunkX, chunkZ, ByteSlice.of(new ByteWriter().varInt(0).toByteArray()),
            List.copyOf(sections), ByteSlice.of(new byte[]{0, 0, 0, 0}));
    }

    private static byte[] biomes() {
        return new ByteWriter().u8(0).varInt(0).toByteArray();
    }

    /// Time the test owns outright, so a ninety second window costs nothing to fill.
    static final class ManualClock implements CaptureClock {

        private static final Instant EPOCH = Instant.parse("2026-08-30T12:00:00Z");

        private long nanos;

        @Override
        public long nanoTime() {
            return nanos;
        }

        @Override
        public Instant instant() {
            return EPOCH.plusNanos(nanos);
        }

        void set(long nanos) {
            this.nanos = nanos;
        }
    }

    /// Collects finished traces off the writer thread, so a test waits for the writer rather than
    /// sleeping and hoping.
    static final class Traces implements CaptureEngine.Completion {

        record Written(Path path, TraceHeader header) {
        }

        private final LinkedBlockingQueue<Written> written = new LinkedBlockingQueue<>();

        @Override
        public void trace(Path path, TraceHeader header) {
            written.add(new Written(path, header));
        }

        Written take() throws InterruptedException {
            var result = written.poll(20, TimeUnit.SECONDS);
            assertNotNull(result, "the writer produced no trace");
            return result;
        }

        void assertNone() throws InterruptedException {
            assertNull(written.poll(200, TimeUnit.MILLISECONDS), "a trace was written");
        }
    }

    /// An executor that holds the writer until the test lets it go, which makes queue overflow and
    /// its counting deterministic.
    static final class Deferred implements Executor {

        private @Nullable Runnable task;

        @Override
        public void execute(Runnable task) {
            this.task = task;
        }

        void release() {
            var pending = task;
            task = null;
            if (pending != null) Thread.ofVirtual().start(pending);
        }
    }
}
