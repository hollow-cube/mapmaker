package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.ReplayPlayer;
import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.*;
import dev.hollowcube.replay.io.*;
import net.hollowcube.mapmaker.map.util.PositionUtil;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.ClearGhostBlocksEvent;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

final class ReplaySessionTest {
    private static final BlockVec GHOST_POINT = new BlockVec(4, 65, -7);
    private static final long GHOST_POSITION = PositionUtil.packPosition(GHOST_POINT);
    /// What the world has wherever a ghost block is not.
    private static final Block.Getter WORLD = (_, _, _, _) -> Block.STONE;

    @Test
    void pauseCommitsAndSuppressesTicksUntilResume() {
        var writer = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(writer, snapshot));

        session.advance();
        session.pause().join();
        session.advance();
        session.resume();
        session.advance();

        var stop = session.stop();
        assertSame(stop, session.stop());
        stop.join();

        assertEquals(List.of("commit:0", "commit:1", "close"), writer.operations);

        var header = headerOf(writer.commits.getLast());
        assertEquals(2, header.tickCount());
        assertEquals(2, header.chunkCount());
    }

    @Test
    void stopLeavesTheRecordingResumableButFinishDoesNot() {
        var stopped = new TestWriter();
        new ReplaySession(snapshot -> newRecorder(stopped, snapshot)).stop().join();
        assertTrue(stopped.commits.stream().noneMatch(SegmentedReplayCommit::finished));

        var finished = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(finished, snapshot));
        session.advance();
        session.complete(0).join();
        assertTrue(finished.commits.getLast().finished());
    }

    @Test
    void aRunTooShortToBeWorthKeepingIsNeverSent() {
        var writer = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(writer, snapshot));
        for (var tick = 0; tick < 19; tick++) session.advance();

        session.complete(20).join();

        // Not an empty commit, and not a finished one: nothing about this run reaches storage at
        // all, so nothing has to be cleaned up later either.
        assertEquals(List.of("close"), writer.operations);
    }

    @Test
    void aRunLongEnoughToKeepIsFinishedAsUsual() {
        var writer = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(writer, snapshot));
        for (var tick = 0; tick < 20; tick++) session.advance();

        session.complete(20).join();

        assertTrue(writer.commits.getLast().finished());
        assertEquals(20, headerOf(writer.commits.getLast()).tickCount());
    }

    @Test
    void aShortRunThatAlreadyCommittedIsKeptRatherThanLost() {
        var writer = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(writer, snapshot));
        session.advance();
        // A pause commits, which is what a spectating or testing detour does to a live run.
        session.pause().join();
        session.resume();
        session.advance();

        session.complete(20).join();

        // Those bytes are already in storage, so finishing is the only honest end for them.
        assertTrue(writer.commits.getLast().finished());
    }

    @Test
    void aShortRunThatIsOnlyStoppedIsStillKept() {
        var writer = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(writer, snapshot));
        session.advance();

        // The player left rather than finished; they may come back and make this a long run.
        session.stop().join();

        assertFalse(writer.commits.isEmpty());
        assertTrue(writer.commits.stream().noneMatch(SegmentedReplayCommit::finished));
    }

    @Test
    void theFirstTerminationWins() {
        var writer = new TestWriter();
        var session = new ReplaySession(snapshot -> newRecorder(writer, snapshot));
        session.advance();

        var stop = session.stop();
        assertSame(stop, session.complete(0));
        stop.join();

        assertTrue(writer.commits.stream().noneMatch(SegmentedReplayCommit::finished));
    }

    @Test
    void resumingReAnchorsInsteadOfDeltaingAcrossTheGap(@TempDir Path temporaryDirectory) {
        var recorded = record(temporaryDirectory, session -> {
            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(0, 64, 0), Vec.ZERO);
            session.advance();
            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(1, 64, 0), Vec.ZERO);
            session.advance();

            session.pause().join();
            session.resume();

            // The subject moved while nothing was recording, so a delta from its last known
            // position would put playback in the wrong place for the rest of the replay.
            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(50, 64, 0), Vec.ZERO);
            session.advance();
        });

        // Each of the two chunks opens with a snapshot, and the first thing a snapshot says is
        // that the world holds none of the blocks a replay may have put in it.
        assertEquals(
            List.of(
                new ClearGhostBlocksEvent(),
                new AbsoluteMoveEvent(0, new Pos(0, 64, 0), Vec.ZERO),
                new DeltaMoveEvent(0, new Pos(1, 0, 0), Vec.ZERO),
                new ClearGhostBlocksEvent(),
                new AbsoluteMoveEvent(0, new Pos(50, 64, 0), Vec.ZERO)
            ),
            recorded
        );
    }

    @Test
    void trackedEntitiesSpawnBeforeTheyMoveAndAreDestroyedWhenTheyGoAway(@TempDir Path temporaryDirectory) {
        var entity = new Entity(EntityType.ARMOR_STAND);

        var recorded = record(temporaryDirectory, session -> {
            var entityId = session.trackEntity(entity);
            assertNotEquals(ReplaySession.SUBJECT_ENTITY_ID, entityId);

            session.captureMovement(entityId, new Pos(3, 64, 3), Vec.ZERO);
            session.advance();

            session.releaseEntitiesOutside(Set.of());
            session.advance();
        });

        assertEquals(4, recorded.size());
        assertEquals(new ClearGhostBlocksEvent(), recorded.getFirst()); // the opening snapshot
        var spawn = assertInstanceOf(SpawnEntityEvent.class, recorded.get(1));
        assertInstanceOf(AbsoluteMoveEvent.class, recorded.get(2));
        assertEquals(new DestroyEntityEvent(spawn.entityId()), recorded.getLast());
    }

    @Test
    void seekingToALaterChunkReconstructsEveryTrackedEntity(@TempDir Path temporaryDirectory) {
        var entity = new Entity(EntityType.ARMOR_STAND);

        var replay = compact(temporaryDirectory, session -> {
            var entityId = session.trackEntity(entity);
            for (var tick = 0; tick < 150; tick++) {
                session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(tick, 64, 0), Vec.ZERO);
                session.captureMovement(entityId, new Pos(0, 64, tick), Vec.ZERO);
                session.advance();
            }
        });

        var chunk = indexOf(replay).get(1);
        assertTrue(chunk.hasSnapshot());

        // The tracked entity was spawned in the first chunk and never mentioned again, so only the
        // snapshot opening this one can put it back.
        assertEquals(
            Map.of(
                ReplaySession.SUBJECT_ENTITY_ID, new Pos(chunk.startTick(), 64, 0),
                ReplaySession.SUBJECT_ENTITY_ID + 1, new Pos(0, 64, chunk.startTick())
            ),
            reconstruct(replay, chunk.startTick() + 1)
        );
    }

    @Test
    void seekingIntoAChunkTruncatedByItsByteLimitStillReconstructsEntities(@TempDir Path temporaryDirectory) {
        var entity = new Entity(EntityType.ARMOR_STAND);

        var replay = compact(temporaryDirectory, session -> {
            var entityId = session.trackEntity(entity);
            for (var tick = 0; tick < 40; tick++) {
                session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(tick, 64, 0), Vec.ZERO);
                session.captureMovement(entityId, new Pos(0, 64, tick), Vec.ZERO);

                // Enough noise to run the chunk past its byte limit long before its tick limit, so
                // the next chunk opens somewhere the session never chose.
                for (var i = 0; i < 20000; i++)
                    session.submit(new HandAnimationEvent(entityId, PlayerHand.MAIN));
                session.advance();
            }
        });

        var index = indexOf(replay);
        assertTrue(index.size() > 1, "expected the byte limit to close a chunk early");
        var chunk = index.get(1);
        assertTrue(chunk.startTick() < 100, "expected the first chunk to be shorter than the tick limit");
        assertTrue(chunk.hasSnapshot());

        assertEquals(
            Map.of(
                ReplaySession.SUBJECT_ENTITY_ID, new Pos(chunk.startTick(), 64, 0),
                ReplaySession.SUBJECT_ENTITY_ID + 1, new Pos(0, 64, chunk.startTick())
            ),
            reconstruct(replay, chunk.startTick() + 1)
        );
    }

    @Test
    void stateItemsItemUseAndBlocksAreOnlyRecordedWhenTheyChange(@TempDir Path temporaryDirectory) {
        var entity = new Entity(EntityType.ZOMBIE);
        var equipment = new TestEquipment();
        var helmet = ItemStack.of(Material.DIAMOND_HELMET);

        var recorded = record(temporaryDirectory, session -> {
            session.captureState(ReplaySession.SUBJECT_ENTITY_ID, entity);
            session.captureEquipment(ReplaySession.SUBJECT_ENTITY_ID, equipment);
            session.captureItemUse(ReplaySession.SUBJECT_ENTITY_ID, null);
            session.captureBlocks(Map.of(GHOST_POSITION, Block.SLIME_BLOCK), WORLD);
            session.advance();

            // An identical tick, which nothing should have anything to say about.
            session.captureState(ReplaySession.SUBJECT_ENTITY_ID, entity);
            session.captureEquipment(ReplaySession.SUBJECT_ENTITY_ID, equipment);
            session.captureItemUse(ReplaySession.SUBJECT_ENTITY_ID, null);
            session.captureBlocks(Map.of(GHOST_POSITION, Block.SLIME_BLOCK), WORLD);
            session.advance();

            entity.setPose(EntityPose.SNEAKING);
            entity.setSneaking(true);
            equipment.setHelmet(helmet);
            session.captureState(ReplaySession.SUBJECT_ENTITY_ID, entity);
            session.captureEquipment(ReplaySession.SUBJECT_ENTITY_ID, equipment);
            session.captureItemUse(ReplaySession.SUBJECT_ENTITY_ID, PlayerHand.OFF);
            session.captureBlocks(Map.of(), WORLD);
            session.advance();
        });

        assertEquals(
            List.of(
                new ClearGhostBlocksEvent(), // the snapshot opening the first chunk
                // The opening tick has nothing to diff against, so it describes everything.
                new EntityStateEvent(0, EntityPose.STANDING, false, false, false, false, false, false, false, false),
                new ItemUseEvent(0, null),
                new SetBlockEvent(GHOST_POINT, Block.SLIME_BLOCK),
                // Every recorded equipment slot at once, because they all changed on the same tick.
                new SetItemEvent(0, Map.of(
                    SetItemEvent.slotOf(EquipmentSlot.HELMET), ItemStack.AIR,
                    SetItemEvent.slotOf(EquipmentSlot.CHESTPLATE), ItemStack.AIR,
                    SetItemEvent.slotOf(EquipmentSlot.LEGGINGS), ItemStack.AIR,
                    SetItemEvent.slotOf(EquipmentSlot.BOOTS), ItemStack.AIR,
                    SetItemEvent.slotOf(EquipmentSlot.OFF_HAND), ItemStack.AIR
                )),

                new EntityStateEvent(0, EntityPose.SNEAKING, false, true, false, false, false, false, false, false),
                new ItemUseEvent(0, PlayerHand.OFF),
                // A ghost block that goes away is recorded as the world's own block returning.
                new SetBlockEvent(GHOST_POINT, Block.STONE),
                new SetItemEvent(0, Map.of(SetItemEvent.slotOf(EquipmentSlot.HELMET), helmet))
            ),
            recorded
        );
    }

    @Test
    void everythingATickDoesToAnInventoryArrivesAsOneEvent(@TempDir Path temporaryDirectory) {
        var recorded = record(temporaryDirectory, session -> {
            session.captureItem(ReplaySession.SUBJECT_ENTITY_ID, 3, ItemStack.of(Material.STONE));
            session.captureItem(ReplaySession.SUBJECT_ENTITY_ID, 4, ItemStack.of(Material.DIRT));
            // A slot touched twice in a tick is only worth whatever it ended the tick holding.
            session.captureItem(ReplaySession.SUBJECT_ENTITY_ID, 3, ItemStack.of(Material.DIAMOND));
            session.advance();

            // Nothing changed, so the whole tick is worth nothing.
            session.captureItem(ReplaySession.SUBJECT_ENTITY_ID, 3, ItemStack.of(Material.DIAMOND));
            session.advance();
        });

        assertEquals(
            List.of(
                new ClearGhostBlocksEvent(), // the snapshot opening the first chunk
                new SetItemEvent(0, Map.of(3, ItemStack.of(Material.DIAMOND), 4, ItemStack.of(Material.DIRT)))
            ),
            recorded
        );
    }

    @Test
    void velocityIsRecordedEvenWhenNothingMoved(@TempDir Path temporaryDirectory) {
        var recorded = record(temporaryDirectory, session -> {
            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(0, 64, 0), new Vec(1, 0, -1));
            session.advance();

            // Standing on the spot with a velocity is exactly the sort of thing worth having.
            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(0, 64, 0), new Vec(0, 1, 0));
            session.advance();

            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, new Pos(0, 64, 0), new Vec(0, 1, 0));
            session.advance();
        });

        assertEquals(
            List.of(
                new ClearGhostBlocksEvent(),
                new AbsoluteMoveEvent(0, new Pos(0, 64, 0), new Vec(1, 0, -1)),
                new DeltaMoveEvent(0, new Pos(0, 0, 0), new Vec(0, 1, 0))
            ),
            recorded
        );
    }

    @Test
    void roundingIsCarriedIntoTheNextDeltaRatherThanAccumulating(@TempDir Path temporaryDirectory) {
        // A step no whole number of 1/4096ths describes, so every tick leaves a remainder behind.
        var step = 0.1 / 3;
        var ticks = 100;
        var recorded = record(temporaryDirectory, session -> {
            for (var tick = 0; tick <= ticks; tick++) {
                session.captureMovement(
                    ReplaySession.SUBJECT_ENTITY_ID, new Pos(step * tick, 64, -step * tick), Vec.ZERO);
                session.advance();
            }
        });

        // Rebuild the position the way playback does, by adding up only what was written down.
        var rebuilt = Pos.ZERO;
        for (var event : recorded) {
            if (event instanceof AbsoluteMoveEvent(var _, var position, var _)) rebuilt = position;
            else if (event instanceof DeltaMoveEvent(var _, var delta, var _)) rebuilt = rebuilt.add(delta);
        }

        // Half a step off, after a hundred ticks of it. Diffing against the true position instead
        // would have let the remainders pile up to roughly forty times this.
        var expected = new Pos(step * ticks, 64, -step * ticks);
        assertEquals(expected.x(), rebuilt.x(), 1 / 8192d);
        assertEquals(expected.y(), rebuilt.y(), 1 / 8192d);
        assertEquals(expected.z(), rebuilt.z(), 1 / 8192d);
    }

    @Test
    void everyVanillaVisibleFlagIsRecorded(@TempDir Path temporaryDirectory) {
        var entity = new LivingEntity(EntityType.ZOMBIE);
        var meta = (LivingEntityMeta) entity.getEntityMeta();

        var recorded = record(temporaryDirectory, session -> {
            meta.setOnFire(true);
            entity.setSneaking(true);
            meta.setInRiptideSpinAttack(true);
            entity.setSprinting(true);
            meta.setSwimming(true);
            entity.setInvisible(true);
            entity.setGlowing(true);
            meta.setFlyingWithElytra(true);
            session.captureState(ReplaySession.SUBJECT_ENTITY_ID, entity);
            session.advance();
        });

        assertEquals(
            List.of(
                new ClearGhostBlocksEvent(),
                new EntityStateEvent(0, entity.getPose(), true, true, true, true, true, true, true, true)
            ),
            recorded
        );
    }

    @Test
    void seekingToALaterChunkRestoresPoseItemsAndBlocks(@TempDir Path temporaryDirectory) {
        var entity = new Entity(EntityType.ZOMBIE);
        entity.setPose(EntityPose.FALL_FLYING);
        var equipment = new TestEquipment();
        equipment.setHelmet(ItemStack.of(Material.DIAMOND_HELMET));

        var replay = compact(temporaryDirectory, session -> {
            for (var tick = 0; tick < 150; tick++) {
                // Only the opening tick says any of this, so only a snapshot can put it back.
                session.captureState(ReplaySession.SUBJECT_ENTITY_ID, entity);
                session.captureEquipment(ReplaySession.SUBJECT_ENTITY_ID, equipment);
                session.captureItemUse(ReplaySession.SUBJECT_ENTITY_ID, PlayerHand.MAIN);
                session.captureBlocks(Map.of(GHOST_POSITION, Block.SLIME_BLOCK), WORLD);
                session.advance();
            }
        });

        var chunk = indexOf(replay).get(1);
        assertTrue(chunk.hasSnapshot());

        var seen = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(replay), ReplayManager.REGISTRY, seen::add)) {
            player.seek(chunk.startTick() + 1);
        }

        assertTrue(seen.contains(
            new EntityStateEvent(0, EntityPose.FALL_FLYING, false, false, false, false, false, false, false, false)));
        assertTrue(seen.stream().anyMatch(event -> event instanceof SetItemEvent(var _, var items)
            && ItemStack.of(Material.DIAMOND_HELMET).equals(items.get(SetItemEvent.slotOf(EquipmentSlot.HELMET)))));
        assertTrue(seen.contains(new ItemUseEvent(0, PlayerHand.MAIN)));
        // The clear has to come first, or ghost blocks an earlier playback position set would
        // survive it.
        assertEquals(
            List.of(new ClearGhostBlocksEvent(), new SetBlockEvent(GHOST_POINT, Block.SLIME_BLOCK)),
            seen.stream().filter(event -> event instanceof ClearGhostBlocksEvent || event instanceof SetBlockEvent).toList()
        );
    }

    private static List<ChunkIndex> indexOf(byte[] replay) {
        try (var reader = new CompactedReplayReader(replay)) {
            return reader.index();
        }
    }

    /// Seeks and applies what comes out the way playback would, so a test can assert on where
    /// entities end up rather than on the events that got them there.
    private static Map<Integer, Pos> reconstruct(byte[] replay, int tick) {
        var positions = new LinkedHashMap<Integer, Pos>();
        // Playback spawns the subject up front, so a delta arriving before anything anchors it is
        // applied to a position no recording ever chose.
        positions.put(ReplaySession.SUBJECT_ENTITY_ID, new Pos(-1000, -1000, -1000));

        var handler = (Consumer<ReplayEvent>) event -> {
            switch (event) {
                case SpawnEntityEvent(int entityId, var _, Pos position) -> positions.put(entityId, position);
                case AbsoluteMoveEvent(int entityId, Pos position, var _) -> positions.put(entityId, position);
                case DeltaMoveEvent(int entityId, Pos delta, var _) -> positions.computeIfPresent(entityId, (_, last) -> last.add(delta));
                case DestroyEntityEvent(int entityId) -> positions.remove(entityId);
                default -> {
                }
            }
        };
        try (var player = new ReplayPlayer(new CompactedReplayReader(replay), ReplayManager.REGISTRY, handler)) {
            player.seek(tick);
        }
        return positions;
    }

    /// Drives a session and reads back what it actually committed, so these assert on the recording
    /// rather than on calls made against a stand-in.
    private static List<ReplayEvent> record(Path root, Consumer<ReplaySession> script) {
        var events = new ArrayList<ReplayEvent>();
        var recorded = compact(root, script);
        try (var player = new ReplayPlayer(new CompactedReplayReader(recorded), ReplayManager.REGISTRY, events::add)) {
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        }
        return events;
    }

    /// Drives a session and returns its compacted replay, for tests that need the chunk index or
    /// want to seek rather than play the whole thing.
    private static byte[] compact(Path root, Consumer<ReplaySession> script) {
        var storage = new SegmentedFileReplayStorage(root);
        var session = new ReplaySession(snapshot -> ReplayRecorder.create(
            ReplayManager.REGISTRY,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            snapshot
        ));

        script.accept(session);
        session.complete(0).join();

        var recording = storage.load("run");
        assertNotNull(recording);
        return ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(root.resolve("run"))
        ).data();
    }

    private static ReplayRecorder newRecorder(SegmentedReplayWriter writer, Runnable snapshot) {
        return ReplayRecorder.create(
            ReplayManager.REGISTRY,
            writer,
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            snapshot
        );
    }

    private static ReplayHeader headerOf(SegmentedReplayCommit commit) {
        var preamble = MemorySegment.ofArray(commit.preamble());
        return new ReplayHeader(NetworkBuffer.wrap(preamble, 0, preamble.byteSize()));
    }

    /// Stands in for an entity's equipment, which a real one cannot hold without a running server.
    private static final class TestEquipment implements EquipmentHandler {
        private final Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);

        @Override
        public ItemStack getEquipment(EquipmentSlot slot) {
            return items.getOrDefault(slot, ItemStack.AIR);
        }

        @Override
        public void setEquipment(EquipmentSlot slot, ItemStack itemStack) {
            items.put(slot, itemStack);
        }
    }

    private static final class TestWriter implements SegmentedReplayWriter {
        private final List<String> operations = new ArrayList<>();
        private final List<SegmentedReplayCommit> commits = new ArrayList<>();

        @Override
        public void commit(SegmentedReplayCommit commit) {
            operations.add(commit.hasSegment() ? "commit:" + commit.segmentIndex() : "commit");
            commits.add(commit);
        }

        @Override
        public void close() {
            operations.add("close");
        }
    }
}
