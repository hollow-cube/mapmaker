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
        recorder.submit(new SpawnEntityEvent(1, EntityType.ARMOR_STAND, new Pos(5, 64, 5)));
        recorder.submit(new DestroyEntityEvent(1));
        expected.add(new SpawnEntityEvent(1, EntityType.ARMOR_STAND, new Pos(5, 64, 5)));
        expected.add(new DestroyEntityEvent(1));
        recorder.advance();
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

        var expected = List.<ReplayEvent>of(
            new DeltaMoveEvent(0, new Pos(1, 2, 3, 4, 5), new Vec(1, 0, -1)),
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
