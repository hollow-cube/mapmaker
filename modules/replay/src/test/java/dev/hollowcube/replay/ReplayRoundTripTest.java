package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.*;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// Records a replay, compacts it, and plays it back, so the three halves of the format stay
/// honest about each other.
final class ReplayRoundTripTest {

    @Test
    void recordedEventsSurviveCompactionAndPlaybackInOrder(@TempDir Path temporaryDirectory) {
        var registry = ReplayEvents.builder().build();
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        var recorder = ReplayRecorder.create(
            registry,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );

        // Enough ticks to span several chunks, so playback has to cross chunk boundaries.
        var expected = new ArrayList<ReplayEvent>();
        for (var tick = 0; tick < 250; tick++) {
            var event = tick % 50 == 0
                ? new AbsoluteMoveEvent(0, new Pos(tick, 64, 0), Vec.ZERO)
                : new DeltaMoveEvent(0, new Pos(1, 0, 0), Vec.ZERO);
            recorder.submit(event);
            expected.add(event);
            recorder.advance();
        }
        // Submitted with no advance() after them, the way run-ending events reach a recorder.
        recorder.submit(new SpawnEntityEvent(1, EntityType.ARMOR_STAND, new Pos(5, 64, 5)));
        recorder.submit(new DestroyEntityEvent(1));
        expected.add(new SpawnEntityEvent(1, EntityType.ARMOR_STAND, new Pos(5, 64, 5)));
        expected.add(new DestroyEntityEvent(1));
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        assertTrue(recording.finished());
        assertEquals(251, recording.requirePreamble().header().tickCount());

        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(compacted.data()), registry, played::add)) {
            assertEquals(251, player.tickCount());
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
            assertEquals(251, player.tick());
        }

        assertEquals(expected, played);
    }

    @Test
    void everyGenericEventSurvivesTheFormat(@TempDir Path temporaryDirectory) {
        var registry = ReplayEvents.builder().build();
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        // The delta's coordinates are whole 1/4096ths and its view whole 1/65536ths of a turn, so
        // that this stays a test of the format rather than of how LP_POS rounds; that has its own
        // test below.
        var expected = List.<ReplayEvent>of(
            new DeltaMoveEvent(0, new Pos(1, 2, 3, 5.625f, -45f), new Vec(1, 0, -1)),
            new SetItemEvent(0, Map.of(4, ItemStack.of(Material.DIAMOND, 12))),
            new SetItemEvent(0, Map.of(
                4, ItemStack.of(Material.DIAMOND, 12),
                SetItemEvent.slotOf(EquipmentSlot.HELMET), ItemStack.of(Material.DIAMOND_HELMET),
                SetItemEvent.slotOf(EquipmentSlot.OFF_HAND), ItemStack.AIR
            )),
            new ChangeHeldSlotEvent(0, 4),
            new HandAnimationEvent(0, PlayerHand.OFF),
            new SpawnEntityEvent(1, EntityType.ARMOR_STAND, new Pos(5, 64, 5)),
            new DestroyEntityEvent(1),
            new AbsoluteMoveEvent(0, new Pos(9, 8, 7, 6, 5), new Vec(-1, 1, 0)),
            new EntityStateEvent(0, EntityPose.SWIMMING, true, false, true, false, true, false, true, false),
            new EntityStateEvent(1, EntityPose.STANDING, false, true, false, true, false, true, false, true),
            new ItemUseEvent(0, PlayerHand.OFF),
            new ItemUseEvent(0, null),
            new SetBlockEvent(new BlockVec(-4, 70, 12), Block.OAK_STAIRS.withProperty("facing", "west"))
        );

        var recorder = ReplayRecorder.create(
            registry,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        for (var event : expected) recorder.submit(event);
        recorder.advance();
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(compacted.data()), registry, played::add)) {
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        }

        assertEquals(expected, played);
    }

    @Test
    void velocitySurvivesTheFormatToWithinItsPrecision(@TempDir Path temporaryDirectory) {
        var registry = ReplayEvents.builder().build();
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        var recorder = ReplayRecorder.create(
            registry,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        recorder.submit(new DeltaMoveEvent(0, new Pos(0.1, 0, 0.2), new Vec(0.31, -0.0784, -0.12)));
        recorder.submit(new AbsoluteMoveEvent(0, new Pos(4, 64, 4), Vec.ZERO));
        recorder.advance();
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(compacted.data()), registry, played::add)) {
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        }

        // The wire quantizes a velocity, so it comes back near enough rather than identical.
        var delta = assertInstanceOf(DeltaMoveEvent.class, played.getFirst());
        assertEquals(0.31, delta.velocity().x(), 1e-4);
        assertEquals(-0.0784, delta.velocity().y(), 1e-4);
        assertEquals(-0.12, delta.velocity().z(), 1e-4);
        // Zero is the one value it is exact about, which is what makes a still entity cost a byte.
        assertEquals(Vec.ZERO, assertInstanceOf(AbsoluteMoveEvent.class, played.getLast()).velocity());
    }

    @Test
    void aDeltaSurvivesTheFormatToWithinItsPrecisionAndAnAnchorExactly(@TempDir Path temporaryDirectory) {
        var registry = ReplayEvents.builder().build();
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        var recorder = ReplayRecorder.create(
            registry,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        recorder.submit(new DeltaMoveEvent(0, new Pos(0.0731, -0.0784, 0.2, 137.4f, -22.9f), Vec.ZERO));
        recorder.submit(new AbsoluteMoveEvent(0, new Pos(-3184.61, 71.9375, 902.03, -179.9f, 90f), Vec.ZERO));
        recorder.advance();
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(compacted.data()), registry, played::add)) {
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        }

        // A delta's coordinate lands on the nearer 1/4096 of a block.
        var delta = assertInstanceOf(DeltaMoveEvent.class, played.getFirst()).delta();
        assertEquals(0.0731, delta.x(), 1.0 / 8192);
        assertEquals(-0.0784, delta.y(), 1.0 / 8192);
        assertEquals(0.2, delta.z(), 1.0 / 8192);
        assertNotEquals(0.0731, delta.x(), "the delta should have been quantized at all");

        // Its view lands on the nearer 1/65536 of a turn, 256 times finer than the angle byte
        // watching a player live would have shown.
        assertEquals(137.4f, delta.yaw(), 360f / 131072);
        assertEquals(-22.9f, delta.pitch(), 360f / 131072);

        // An anchor is exact, so the rounding above is corrected rather than accumulated.
        assertEquals(
            new Pos(-3184.61, 71.9375, 902.03, -179.9f, 90f),
            assertInstanceOf(AbsoluteMoveEvent.class, played.getLast()).position()
        );
    }

    @Test
    void aViewSurvivesAYawNoClientEverNormalized() {
        // A client reports the yaw it has accumulated, so a player who keeps turning sends one that
        // grows without bound. It has to come back pointing the same way however far it has gone.
        for (var turns = 0; turns < 400; turns++) {
            var yaw = 137.4f + 360f * turns;
            var written = NetworkBuffer.makeArray(ReplayTypes.LP_POS, new Pos(0, 0, 0, yaw, -22.9f));
            var read = NetworkBuffer.wrap(written, 0, written.length).read(ReplayTypes.LP_POS);

            // The tolerance grows with the yaw only because the float the client sent has itself
            // run out of resolution by then. What this encodes stays half a step off, always.
            var tolerance = 360f / 131072 + Math.ulp(yaw);
            assertEquals(137.4f, Pos.fixYaw(read.yaw()), tolerance, "yaw after " + turns + " turns");
            assertEquals(-22.9f, read.pitch(), 360f / 131072);
        }

        // Well past where a half float would have run out of exponent and become infinite.
        var extreme = NetworkBuffer.makeArray(ReplayTypes.LP_POS, new Pos(0, 0, 0, 200_000f, 90f));
        var read = NetworkBuffer.wrap(extreme, 0, extreme.length).read(ReplayTypes.LP_POS);
        assertTrue(Float.isFinite(read.yaw()), "a yaw this large must still be an angle");
        assertEquals(90f, read.pitch(), 360f / 131072);
    }

    @Test
    void seekingRewindsToTheOwningChunkAndReplaysForward(@TempDir Path temporaryDirectory) {
        var registry = ReplayEvents.builder().build();
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        var recorder = ReplayRecorder.create(
            registry,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        for (var tick = 0; tick < 250; tick++) {
            recorder.submit(new AbsoluteMoveEvent(0, new Pos(tick, 64, 0), Vec.ZERO));
            recorder.advance();
        }
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(compacted.data()), registry, played::add)) {
            player.seek(180);
            assertEquals(180, player.tick());

            played.clear();
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(new AbsoluteMoveEvent(0, new Pos(180, 64, 0), Vec.ZERO)), played);

            player.seek(0);
            played.clear();
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(new AbsoluteMoveEvent(0, new Pos(0, 64, 0), Vec.ZERO)), played);
        }
    }
}
