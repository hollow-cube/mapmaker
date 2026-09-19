package net.hollowcube.apiworker.jobs;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.ReplayPlayer;
import dev.hollowcube.replay.ReplayVisitor;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.SetBlockEvent;
import dev.hollowcube.replay.event.SpawnEntityEvent;
import dev.hollowcube.replay.io.CompactedReplayReader;
import net.hollowcube.mapmaker.runtime.parkour.replay.ReplayManager;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/// Format 4 replays, built by hand since nothing can write one any more.
final class LegacyReplays {

    /// Event IDs in `ReplayEvents#builder()`.
    private static final int SET_ITEM = 5, SET_BLOCK = 7, SPAWN_ENTITY = 8;
    /// As 26.2 assigned them.
    private static final int STRAY_26_2 = 128;
    private static final int OAK_STAIRS_NORTH_TOP_STRAIGHT_WATERLOGGED_26_2 = 3907;

    static final List<ReplayEvent> EVENTS = List.of(
        new SpawnEntityEvent(1, EntityType.STRAY, new Pos(5, 64, 5)),
        new SetBlockEvent(
            new BlockVec(1, 2, 3),
            Block.OAK_STAIRS.withProperties(
                Map.of("facing", "north", "half", "top", "shape", "straight", "waterlogged", "true")
            )
        )
    );

    static final List<byte[]> TICKS = List.of(
        tick(0, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SPAWN_ENTITY);
            buffer.write(NetworkBuffer.VAR_INT, 1);
            buffer.write(NetworkBuffer.VAR_INT, STRAY_26_2);
            buffer.write(NetworkBuffer.POS, new Pos(5, 64, 5));
        }),
        tick(1, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SET_BLOCK);
            buffer.write(NetworkBuffer.BLOCK_POSITION, new BlockVec(1, 2, 3));
            buffer.write(NetworkBuffer.VAR_INT, OAK_STAIRS_NORTH_TOP_STRAIGHT_WATERLOGGED_26_2);
        }, buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, SET_ITEM);
            buffer.write(NetworkBuffer.VAR_INT, 0);
            buffer.write(
                NetworkBuffer.VAR_INT.mapValue(NetworkBuffer.NBT_COMPOUND),
                Map.of(
                    4,
                    CompoundBinaryTag.builder()
                        .putString("id", "minecraft:diamond")
                        .putInt("count", 12)
                        .build()
                )
            );
        })
    );

    private LegacyReplays() {}

    static Recording recording() {
        var segments = TICKS.stream().map(Zstd::compress).toList();
        var chunks = new ArrayList<long[]>();
        for (var i = 0; i < TICKS.size(); i++)
            chunks.add(new long[] {i, (long) i << 32, segments.get(i).length, TICKS.get(i).length});
        return new Recording(preamble(chunks), segments);
    }

    record Recording(byte[] preamble, List<byte[]> segments) {}

    static byte[] compacted() {
        var frames = TICKS.stream().map(Zstd::compress).toList();
        // The offset is fixed width, so the preamble is the same length wherever it points.
        var preambleLength = preamble(offsets(frames, 0)).length;
        var preamble = preamble(offsets(frames, preambleLength));
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.RAW_BYTES, preamble);
            for (var frame : frames) buffer.write(NetworkBuffer.RAW_BYTES, frame);
        });
    }

    private static List<long[]> offsets(List<byte[]> frames, long start) {
        var chunks = new ArrayList<long[]>();
        var offset = start;
        for (var i = 0; i < frames.size(); i++) {
            chunks.add(new long[] {i, offset, frames.get(i).length, TICKS.get(i).length});
            offset += frames.get(i).length;
        }
        return chunks;
    }

    static List<ReplayEvent> play(byte[] compacted) {
        var played = new ArrayList<ReplayEvent>();
        try (
            var player = new ReplayPlayer(
                new CompactedReplayReader(compacted),
                ReplayManager.REGISTRY,
                played::add
            )
        ) {
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) {}
        }
        return played;
    }

    static List<ReplayEvent> events(byte[] compacted) {
        var events = new ArrayList<ReplayEvent>();
        try (var reader = new CompactedReplayReader(compacted)) {
            ReplayVisitor.walk(reader, (chunk, decoded) -> {
                for (var tick = 0; tick < chunk.tickCount(); tick++) {
                    decoded.read(NetworkBuffer.VAR_INT);
                    var count = decoded.read(NetworkBuffer.SHORT);
                    for (var event = 0; event < count; event++)
                        events.add(ReplayManager.REGISTRY.read(decoded, chunk));
                }
            });
        }
        return events;
    }

    @SafeVarargs
    private static byte[] tick(int tick, Consumer<NetworkBuffer>... events) {
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, tick);
            buffer.write(NetworkBuffer.SHORT, (short) events.length);
            for (var event : events) event.accept(buffer);
        });
    }

    /// `chunks` as `{startTick, byteOffset, compressedLength, uncompressedLength}`, one tick each.
    private static byte[] preamble(List<long[]> chunks) {
        var metadata = NetworkBuffer.makeArray(
            NetworkBuffer.NBT_COMPOUND,
            CompoundBinaryTag.empty()
        );
        var index = NetworkBuffer.makeArray(buffer -> {
            for (var chunk : chunks) {
                buffer.write(NetworkBuffer.VAR_INT, (int) chunk[0]);
                buffer.write(NetworkBuffer.VAR_INT, 1);
                buffer.write(NetworkBuffer.BYTE, ChunkIndex.FLAG_HAS_SNAPSHOT);
                buffer.write(NetworkBuffer.LONG, chunk[1]);
                buffer.write(NetworkBuffer.VAR_INT, (int) chunk[2]);
                buffer.write(NetworkBuffer.VAR_INT, (int) chunk[3]);
            }
        });
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.INT, ReplayHeader.MAGIC);
            buffer.write(NetworkBuffer.SHORT, ReplayHeader.VERSION_LEGACY_IDS);
            buffer.write(NetworkBuffer.SHORT, (short) 0);
            buffer.write(NetworkBuffer.UUID, UUID.randomUUID());
            buffer.write(NetworkBuffer.BYTE, (byte) 0);
            buffer.write(NetworkBuffer.LONG, 0L);
            buffer.write(NetworkBuffer.SHORT, (short) 0);
            buffer.write(NetworkBuffer.INT, metadata.length);
            buffer.write(NetworkBuffer.INT, index.length);
            buffer.write(NetworkBuffer.INT, chunks.size());
            buffer.write(NetworkBuffer.INT, chunks.size());
            buffer.write(NetworkBuffer.INT, ReplayHeader.LEGACY_IDS_DATA_VERSION);
            buffer.advanceWrite(ReplayHeader.HEADER_LENGTH - buffer.writeIndex());
            buffer.write(NetworkBuffer.RAW_BYTES, metadata);
            buffer.write(NetworkBuffer.RAW_BYTES, index);
        });
    }
}
