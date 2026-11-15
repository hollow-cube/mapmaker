package net.hollowcube.mapmaker.map.wal;

import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalBatchTest {
    private static NetworkBuffer.Type<EditBuffer> edits;

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
        edits = EditBuffer.serializer(GameDataProvider.NOOP, MinecraftServer.getBiomeRegistry());
    }

    @Test
    void roundTripsEveryRecordType() {
        var sign = Block.OAK_SIGN.withNbt(CompoundBinaryTag.builder().putString("text", "hi").build());
        var buffer = EditBuffer.builder()
            .setBlockState(1, 40, 2, Block.STONE.stateId())
            .setBlockState(3, 41, 4, sign)
            .build();
        var batch = new WalBatch(7, List.of(
            new WalRecord.Edit(1_000, buffer),
            new WalRecord.WorldTag(1_250, 4000, "spawn", StringBinaryTag.stringBinaryTag("value")),
            new WalRecord.WorldTag(1_250, 4000, "gone", null)
        ));

        var bytes = batch.encode(edits);
        var read = WalBatch.decode(NetworkBuffer.wrap(bytes, 0, bytes.length), edits);

        assertEquals(7, read.firstSeq());
        assertEquals(9, read.lastSeq());
        var edit = assertInstanceOf(WalRecord.Edit.class, read.records().get(0));
        assertEquals(1_000, edit.time());
        assertEquals(Block.STONE.stateId(), edit.buffer().getBlockState(1, 40, 2));
        assertEquals(sign.nbt(), edit.buffer().getBlock(3, 41, 4).nbt());
        assertEquals(batch.records().get(1), read.records().get(1));
        assertEquals(batch.records().get(2), read.records().get(2));
    }

    @Test
    void compressesABodyThatShrinks() {
        var box = Area.cuboid(new BlockVec(0, 0, 0), new BlockVec(63, 63, 63));
        var filled = new WalBatch(1, List.of(new WalRecord.Edit(0, EditBuffer.builder().fill(box, Block.STONE.stateId()).build())));
        var single = new WalBatch(1, List.of(new WalRecord.WorldTag(0, 1, "k", null)));

        var filledBytes = filled.encode(edits);
        var singleBytes = single.encode(edits);

        assertEquals(1, filledBytes[1], "zstd");
        assertEquals(0, singleBytes[1], "stored as is");
        var read = WalBatch.decode(NetworkBuffer.wrap(filledBytes, 0, filledBytes.length), edits);
        var edit = assertInstanceOf(WalRecord.Edit.class, read.records().getFirst());
        assertEquals(64, edit.buffer().sectionCount());
        assertEquals(Block.STONE.stateId(), edit.buffer().getBlockState(63, 63, 63));
    }

    @Test
    void batchesAreSelfDelimiting() {
        var first = new WalBatch(1, List.of(new WalRecord.WorldTag(10, 1, "a", null)));
        var second = new WalBatch(2, List.of(new WalRecord.WorldTag(20, 1, "b", null)));
        byte[] a = first.encode(edits), b = second.encode(edits);
        byte[] both = new byte[a.length + b.length];
        System.arraycopy(a, 0, both, 0, a.length);
        System.arraycopy(b, 0, both, a.length, b.length);

        var in = NetworkBuffer.wrap(both, 0, both.length);
        assertEquals(first, WalBatch.decode(in, edits));
        assertEquals(second, WalBatch.decode(in, edits));
        assertEquals(0, in.readableBytes());
    }
}
