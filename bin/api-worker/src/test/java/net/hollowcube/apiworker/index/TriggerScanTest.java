package net.hollowcube.apiworker.index;

import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.polar.PolarChunk;
import net.hollowcube.polar.PolarSection;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Region markers, which neither fixture has: the world is built in memory the way the runtime
/// saves it, with the region as offsets from the marker's position.
class TriggerScanTest {

    private static CompoundBinaryTag marker(String type, double x, double y, double z, double half) {
        var data = CompoundBinaryTag.builder()
            .putString("type", type)
            .put("min", doubles(-half, -half, -half))
            .put("max", doubles(half, half, half))
            .build();
        return CompoundBinaryTag.builder()
            .putString("id", "minecraft:marker")
            .put("Pos", doubles(x, y, z))
            .put("data", data)
            .build();
    }

    private static ListBinaryTag doubles(double... values) {
        var list = ListBinaryTag.builder(BinaryTagTypes.DOUBLE);
        for (var value : values) list.add(DoubleBinaryTag.doubleBinaryTag(value));
        return list.build();
    }

    private static PolarWorld world(CompoundBinaryTag... entities) {
        var chunkData = CompoundBinaryTag.builder().put("entities", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, List.of(entities))).build();
        var chunkUserData = NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, ReadWorldAccess.VERSION_LATEST);
            buffer.write(NetworkBuffer.NBT, chunkData);
        });
        var worldUserData = NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.BYTE, (byte) ReadWorldAccess.VERSION_LATEST);
            buffer.write(NetworkBuffer.VAR_INT, DataFixer.maxVersion());
            buffer.write(NetworkBuffer.NBT, CompoundBinaryTag.empty());
        });
        var sections = new PolarSection[24];
        for (int i = 0; i < sections.length; i++) sections[i] = new PolarSection();
        var chunk = new PolarChunk(0, 0, sections, List.of(), new int[0][], chunkUserData);
        return new PolarWorld(PolarWorld.LATEST_VERSION, 0, PolarWorld.DEFAULT_COMPRESSION, (byte) -4, (byte) 19, worldUserData, List.of(chunk));
    }

    @Test
    void regionsAreOffsetsFromTheMarker_soDistantFinishesDoNotMerge() {
        MapIndexer.init();
        var scan = TriggerScan.scan(world(
            marker("mapmaker:finish", 100, 64, 0, 2),
            marker("mapmaker:finish", 500, 64, 0, 2),
            marker("mapmaker:finish", 502, 64, 0, 2)));

        var finishes = scan.triggers().stream().filter(t -> t.kind() == TriggerScan.Kind.FINISH).toList();
        assertEquals(2, finishes.size(), "the two touching ones are one shape, the far one is not");
        assertEquals(DataFixer.maxVersion(), scan.dataVersion());
        assertEquals(0, scan.decodeFailures());
        assertEquals(0, scan.entities(), "markers are not decoration");
    }

    @Test
    void checkpointsKeepTheirPositions() {
        MapIndexer.init();
        var scan = TriggerScan.scan(world(
            marker("mapmaker:checkpoint", 0, 64, 0, 1),
            marker("mapmaker:checkpoint", 20, 64, 0, 1),
            marker("mapmaker:checkpoint", 20, 64, 0, 1)));

        var checkpoints = scan.triggers().stream().filter(t -> t.kind() == TriggerScan.Kind.CHECKPOINT).toList();
        assertEquals(3, checkpoints.size(), "overlapping checkpoints are deliberately two");
        assertEquals(20, checkpoints.stream().mapToDouble(TriggerScan.Trigger::x).max().orElseThrow(), 1e-9);
    }
}
