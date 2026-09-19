package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.ReplayVisitor;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.event.*;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

final class ReplayUpgradeTest {
    private static final ReplayEventRegistry REGISTRY = ReplayEvents.builder().build();
    private record Versions(int format, int data) {
        static Versions of(ChunkIndex chunk) {
            return new Versions(chunk.formatVersion(), chunk.dataVersion());
        }
    }

    private static final Versions LEGACY = new Versions(ReplayHeader.VERSION_LEGACY_IDS, ReplayHeader.LEGACY_IDS_DATA_VERSION);
    private static final Versions CURRENT = new Versions(ReplayHeader.VERSION_LATEST, MinecraftServer.DATA_VERSION);

    /// Event IDs in [ReplayEvents#builder()].
    private static final int HAND_ANIMATION = 3, SET_ITEM = 5, CHANGE_HELD_SLOT = 6, SET_BLOCK = 7, SPAWN_ENTITY = 8;

    /// IDs as 26.2 assigned them.
    private static final int STRAY_26_2 = 128;
    private static final int OAK_STAIRS_NORTH_TOP_STRAIGHT_WATERLOGGED_26_2 = 3907;

    private static final Block OAK_STAIRS = Block.OAK_STAIRS.withProperties(Map.of(
        "facing", "north", "half", "top", "shape", "straight", "waterlogged", "true"));

    @Test
    void aLegacyCompactedReplayPlaysBackByName() {
        try (var reader = new CompactedReplayReader(legacyCompacted())) {
            assertEquals(ReplayHeader.VERSION_LEGACY_IDS, reader.header().version());
            assertEquals(LEGACY, Versions.of(reader.index().getFirst()));
            assertLegacyEvents(play(reader));
        }
    }

    @Test
    void aLegacyRecordingResumedAfterTheUpgradeKeepsBothEncodings(@TempDir Path directory) throws IOException {
        var worldId = UUID.randomUUID();
        var worldVersion = ReplayHeader.worldVersion(UUID.randomUUID());
        var ticks = legacyTick();
        var segment = Zstd.compress(ticks);
        var recording = directory.resolve("run");
        Files.createDirectories(recording);
        Files.write(recording.resolve("segment-000.dat"), segment);
        Files.write(recording.resolve("preamble.dat"), legacyPreamble(worldId, worldVersion, 1, List.of(
            new LegacyChunk(0, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, 0, segment.length, ticks.length))));

        var storage = new SegmentedFileReplayStorage(directory);
        var legacy = storage.load("run");
        assertNotNull(legacy);
        assertEquals(LEGACY, Versions.of(legacy.requirePreamble().index().getFirst()));

        var recorder = ReplayRecorder.resume(REGISTRY, storage.writer("run", legacy),
            legacy.requirePreamble(), worldId, worldVersion, () -> {});
        var spawn = new SpawnEntityEvent(2, EntityType.ALLAY, new Pos(1, 64, 1));
        var block = new SetBlockEvent(new BlockVec(4, 5, 6), Block.CHERRY_PLANKS);
        recorder.submit(spawn);
        recorder.submit(block);
        recorder.advance();
        recorder.finish(RunOutcome.COMPLETED).join();

        var resumed = storage.load("run");
        assertNotNull(resumed);
        var preamble = resumed.requirePreamble();
        assertEquals(ReplayHeader.VERSION_LATEST, preamble.header().version());
        assertEquals(ReplayHeader.LEGACY_IDS_DATA_VERSION, preamble.header().dataVersion());
        assertEquals(List.of(LEGACY, CURRENT),
            preamble.index().stream().map(Versions::of).toList());

        // Both chunks fit one frame, so only their versions keep them apart.
        var compacted = ReplayCompactor.compact(preamble, new SegmentedFileReplaySource(recording));
        try (var reader = new CompactedReplayReader(compacted.data())) {
            assertEquals(List.of(LEGACY, CURRENT),
                reader.index().stream().map(Versions::of).toList());

            var played = play(reader);
            assertLegacyEvents(played.subList(0, 5));
            assertEquals(List.of(spawn, block), played.subList(5, played.size()));
        }
    }

    @Test
    void aLegacyCompactedReplayTranscodesToOneThatPlaysBackTheSame() {
        var file = legacyCompacted();

        var transcoded = ReplayCompactor.transcode(file, null, REGISTRY);

        assertEquals(Set.of(LEGACY), encodings(file));
        assertEquals(Set.of(CURRENT), encodings(transcoded.data()));
        try (var original = new CompactedReplayReader(file); var reader = new CompactedReplayReader(transcoded.data())) {
            assertEquals(ReplayHeader.VERSION_LATEST, reader.header().version());
            assertEquals(original.header().tickCount(), reader.header().tickCount());
            assertEquals(original.metadata(), reader.metadata());
            var played = play(reader);
            assertLegacyEvents(played);
            assertEquals(played, events(reader));
            assertEquals(play(original), played);
        }
    }

    @Test
    void aLegacyRecordingResumedAfterTheUpgradeTranscodesToOneEncoding(@TempDir Path directory) throws IOException {
        var worldId = UUID.randomUUID();
        var worldVersion = ReplayHeader.worldVersion(UUID.randomUUID());
        var ticks = legacyTick();
        var segment = Zstd.compress(ticks);
        var recording = directory.resolve("run");
        Files.createDirectories(recording);
        Files.write(recording.resolve("segment-000.dat"), segment);
        Files.write(recording.resolve("preamble.dat"), legacyPreamble(worldId, worldVersion, 1, List.of(
            new LegacyChunk(0, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, 0, segment.length, ticks.length))));

        var storage = new SegmentedFileReplayStorage(directory);
        var legacy = storage.load("run");
        assertNotNull(legacy);
        var recorder = ReplayRecorder.resume(REGISTRY, storage.writer("run", legacy),
            legacy.requirePreamble(), worldId, worldVersion, () -> {});
        var spawn = new SpawnEntityEvent(2, EntityType.ALLAY, new Pos(1, 64, 1));
        var block = new SetBlockEvent(new BlockVec(4, 5, 6), Block.CHERRY_PLANKS);
        recorder.submit(spawn);
        recorder.submit(block);
        recorder.advance();
        recorder.finish(RunOutcome.COMPLETED).join();
        var resumed = storage.load("run");
        assertNotNull(resumed);
        var source = new SegmentedFileReplaySource(recording);

        var observed = new ArrayList<ChunkIndex>();
        var transcoded = ReplayCompactor.transcode(resumed.requirePreamble(), source,
            (chunk, decoded) -> observed.add(chunk), REGISTRY);

        assertEquals(List.of(CURRENT, CURRENT),
            observed.stream().map(Versions::of).toList());
        var compacted = ReplayCompactor.compact(storage.load("run").requirePreamble(), source);
        try (var reader = new CompactedReplayReader(transcoded.data());
             var untranscoded = new CompactedReplayReader(compacted.data())) {
            assertEquals(Set.of(CURRENT), encodings(transcoded.data()));
            var played = play(reader);
            assertLegacyEvents(played.subList(0, 5));
            assertEquals(List.of(spawn, block), played.subList(5, played.size()));
            assertEquals(play(untranscoded), played);
            assertEquals(played, events(reader));
        }
    }

    @Test
    void aLegacyRecordingRewrittenInPlaceResumesAfterItsNewSegment(@TempDir Path directory) throws IOException {
        var worldId = UUID.randomUUID();
        var worldVersion = ReplayHeader.worldVersion(UUID.randomUUID());
        var ticks = legacyTick();
        var segment = Zstd.compress(ticks);
        var recording = directory.resolve("run");
        Files.createDirectories(recording);
        Files.write(recording.resolve("segment-000.dat"), segment);
        Files.write(recording.resolve("preamble.dat"), legacyPreamble(worldId, worldVersion, 1, List.of(
            new LegacyChunk(0, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, 0, segment.length, ticks.length))));

        var storage = new SegmentedFileReplayStorage(directory);
        var source = new SegmentedFileReplaySource(recording);
        var rewrite = ReplayCompactor.transcodeRecording(
            storage.load("run").requirePreamble(), source, 1, REGISTRY);
        Files.write(recording.resolve("segment-001.dat"), rewrite.segment());
        Files.write(recording.resolve("preamble.dat"), rewrite.preamble());

        var rewritten = storage.load("run");
        assertNotNull(rewritten);
        var preamble = rewritten.requirePreamble();
        assertEquals(List.of(CURRENT), preamble.index().stream().map(Versions::of).toList());
        assertEquals(1, ReplayPreamble.segmentIndex(preamble.index().getFirst()));
        assertEquals(2, preamble.nextSegmentIndex());

        var recorder = ReplayRecorder.resume(REGISTRY, storage.writer("run", rewritten),
            preamble, worldId, worldVersion, () -> {});
        var spawn = new SpawnEntityEvent(2, EntityType.ALLAY, new Pos(1, 64, 1));
        var block = new SetBlockEvent(new BlockVec(4, 5, 6), Block.CHERRY_PLANKS);
        recorder.submit(spawn);
        recorder.submit(block);
        recorder.advance();
        recorder.finish(RunOutcome.COMPLETED).join();

        var resumed = storage.load("run").requirePreamble();
        assertEquals(List.of(1, 2), resumed.index().stream().map(ReplayPreamble::segmentIndex).toList());
        var compacted = ReplayCompactor.compact(resumed, source);
        try (var reader = new CompactedReplayReader(compacted.data())) {
            assertEquals(Set.of(CURRENT), encodings(compacted.data()));
            var played = play(reader);
            assertLegacyEvents(played.subList(0, 5));
            assertEquals(List.of(spawn, block), played.subList(5, played.size()));
        }
    }

    @Test
    void eachChunkIsBroughtForwardFromItsOwnDataVersion() {
        // Named as they were before chain became iron_chain, and illager_beast became ravager.
        
        var chainTick = tick(0, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SET_BLOCK);
            buffer.write(NetworkBuffer.BLOCK_POSITION, new BlockVec(0, 64, 0));
            buffer.write(NetworkBuffer.STRING, "minecraft:chain[axis=y,waterlogged=false]");
        }, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SET_ITEM);
            buffer.write(NetworkBuffer.VAR_INT, 0);
            buffer.write(NetworkBuffer.VAR_INT.mapValue(NetworkBuffer.NBT_COMPOUND), Map.of(3,
                CompoundBinaryTag.builder().putString("id", "minecraft:chain").putInt("count", 2).build()));
        });
        
        var beastTick = tick(1, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SPAWN_ENTITY);
            buffer.write(NetworkBuffer.VAR_INT, 1);
            buffer.write(NetworkBuffer.STRING, "minecraft:illager_beast");
            buffer.write(NetworkBuffer.POS, Pos.ZERO);
        });

        var chainSegment = Zstd.compress(chainTick);
        var beastSegment = Zstd.compress(beastTick);
        var header = new ReplayHeader(UUID.randomUUID(), new byte[0]);
        var index = List.of(
            new ChunkIndex(0, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, 0, chainSegment.length, chainTick.length,
                ReplayHeader.VERSION_LATEST, 4540),
            new ChunkIndex(1, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, 1L << 32, beastSegment.length, beastTick.length,
                ReplayHeader.VERSION_LATEST, 1927));
        header.update(0, 0, 2, index.size());
        var preamble = new ReplayPreamble(header, CompoundBinaryTag.empty(), index);

        var expected = List.of(
            new SetBlockEvent(new BlockVec(0, 64, 0), Block.IRON_CHAIN.withProperty("axis", "y")),
            new SetItemEvent(0, Map.of(3, ItemStack.of(Material.IRON_CHAIN, 2))),
            new SpawnEntityEvent(1, EntityType.RAVAGER, Pos.ZERO));
        IntFunction<byte[]> segments = segment -> segment == 0 ? chainSegment : beastSegment;
        var compacted = ReplayCompactor.compact(preamble, segments);
        try (var reader = new CompactedReplayReader(compacted.data())) {
            assertEquals(expected, play(reader));
        }
    }

    @Test
    void aNameThatNoLongerResolvesFailsRatherThanPlayingAsSomethingElse() {
        var buffer = NetworkBuffer.resizableBuffer();
        buffer.write(NetworkBuffer.BLOCK_POSITION, BlockVec.ZERO);
        buffer.write(NetworkBuffer.STRING, "minecraft:not_a_block");
        var failure = assertThrows(IllegalStateException.class,
            () -> SetBlockEvent.CODEC.read(buffer, new ChunkIndex(0, 1, (byte) 0, 0, 1, 1)));
        assertTrue(failure.getMessage().contains("minecraft:not_a_block"), failure.getMessage());
    }

    private static void assertLegacyEvents(List<ReplayEvent> played) {
        assertEquals(5, played.size());
        assertEquals(new SpawnEntityEvent(1, EntityType.STRAY, new Pos(5, 64, 5)), played.get(0));
        assertEquals(new SetBlockEvent(new BlockVec(1, 2, 3), OAK_STAIRS), played.get(1));
        assertEquals(PlayerHand.OFF, assertInstanceOf(HandAnimationEvent.class, played.get(2)).hand());
        assertEquals(new SetItemEvent(0, Map.of(4, ItemStack.of(Material.DIAMOND, 12))), played.get(3));
        assertEquals(new ChangeHeldSlotEvent(0, 4), played.get(4));
    }

    private static Set<Versions> encodings(byte[] replay) {
        try (var reader = new CompactedReplayReader(replay)) {
            return reader.index().stream().map(Versions::of).collect(Collectors.toSet());
        }
    }

    /// Every event decoded chunk by chunk rather than through playback.
    private static List<ReplayEvent> events(CompactedReplayReader reader) {
        var events = new ArrayList<ReplayEvent>();
        ReplayVisitor.walk(reader, (chunk, decoded) -> {
            for (var tick = 0; tick < chunk.tickCount(); tick++) {
                decoded.read(NetworkBuffer.VAR_INT);
                var count = decoded.read(NetworkBuffer.SHORT);
                for (var event = 0; event < count; event++) events.add(REGISTRY.read(decoded, chunk));
            }
        });
        return events;
    }

    private static byte[] legacyCompacted() {
        var ticks = legacyTick();
        var compressed = Zstd.compress(ticks);
        // The offset is fixed width, so the preamble is the same length wherever it points.
        var preambleLength = legacyPreamble(UUID.randomUUID(), new byte[0], 1, List.of(
            new LegacyChunk(0, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, 0, compressed.length, ticks.length))).length;
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.RAW_BYTES, legacyPreamble(UUID.randomUUID(), new byte[0], 1, List.of(
                new LegacyChunk(0, 1, ChunkIndex.FLAG_HAS_SNAPSHOT, preambleLength, compressed.length, ticks.length))));
            buffer.write(NetworkBuffer.RAW_BYTES, compressed);
        });
    }

    private static List<ReplayEvent> play(CompactedReplayReader reader) {
        var played = new ArrayList<ReplayEvent>();
        var player = new ReplayPlayer(reader, REGISTRY, played::add);
        while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        return played;
    }

    private static byte[] legacyTick() {
        return tick(0, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SPAWN_ENTITY);
            buffer.write(NetworkBuffer.VAR_INT, 1);
            buffer.write(NetworkBuffer.VAR_INT, STRAY_26_2);
            buffer.write(NetworkBuffer.POS, new Pos(5, 64, 5));
        }, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SET_BLOCK);
            buffer.write(NetworkBuffer.BLOCK_POSITION, new BlockVec(1, 2, 3));
            buffer.write(NetworkBuffer.VAR_INT, OAK_STAIRS_NORTH_TOP_STRAIGHT_WATERLOGGED_26_2);
        }, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, HAND_ANIMATION);
            buffer.write(NetworkBuffer.VAR_INT, 0);
            buffer.write(NetworkBuffer.VAR_INT, PlayerHand.OFF.ordinal());
        }, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SET_ITEM);
            buffer.write(NetworkBuffer.VAR_INT, 0);
            buffer.write(NetworkBuffer.VAR_INT.mapValue(NetworkBuffer.NBT_COMPOUND), Map.of(4,
                ItemStack.of(Material.DIAMOND, 12).toItemNBT(MinecraftServer.process())));
        }, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, CHANGE_HELD_SLOT);
            buffer.write(NetworkBuffer.VAR_INT, 0);
            buffer.write(NetworkBuffer.VAR_INT, 4);
        });
    }

    @SafeVarargs
    private static byte[] tick(int tick, Consumer<NetworkBuffer>... events) {
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, tick);
            buffer.write(NetworkBuffer.SHORT, (short) events.length);
            for (var event : events) event.accept(buffer);
        });
    }

    private record LegacyChunk(int startTick, int tickCount, byte flags, long byteOffset,
                               int compressedLength, int uncompressedLength) {}

    private static byte[] legacyPreamble(UUID worldId, byte[] worldVersion, int tickCount, List<LegacyChunk> chunks) {
        var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, CompoundBinaryTag.empty());
        var index = NetworkBuffer.makeArray(buffer -> {
            for (var chunk : chunks) {
                buffer.write(NetworkBuffer.VAR_INT, chunk.startTick());
                buffer.write(NetworkBuffer.VAR_INT, chunk.tickCount());
                buffer.write(NetworkBuffer.BYTE, chunk.flags());
                buffer.write(NetworkBuffer.LONG, chunk.byteOffset());
                buffer.write(NetworkBuffer.VAR_INT, chunk.compressedLength());
                buffer.write(NetworkBuffer.VAR_INT, chunk.uncompressedLength());
            }
        });
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.INT, ReplayHeader.MAGIC);
            buffer.write(NetworkBuffer.SHORT, ReplayHeader.VERSION_LEGACY_IDS);
            buffer.write(NetworkBuffer.SHORT, (short) 0);
            buffer.write(NetworkBuffer.UUID, worldId);
            buffer.write(NetworkBuffer.BYTE, (byte) worldVersion.length);
            buffer.write(NetworkBuffer.RAW_BYTES, worldVersion);
            buffer.write(NetworkBuffer.LONG, 0L);
            buffer.write(NetworkBuffer.SHORT, (short) 0);
            buffer.write(NetworkBuffer.INT, metadata.length);
            buffer.write(NetworkBuffer.INT, index.length);
            buffer.write(NetworkBuffer.INT, tickCount);
            buffer.write(NetworkBuffer.INT, chunks.size());
            buffer.write(NetworkBuffer.INT, ReplayHeader.LEGACY_IDS_DATA_VERSION);
            buffer.advanceWrite(ReplayHeader.HEADER_LENGTH - buffer.writeIndex());
            buffer.write(NetworkBuffer.RAW_BYTES, metadata);
            buffer.write(NetworkBuffer.RAW_BYTES, index);
        });
    }
}
