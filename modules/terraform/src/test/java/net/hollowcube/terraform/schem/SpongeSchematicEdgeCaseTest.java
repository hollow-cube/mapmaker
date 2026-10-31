package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.clipboard.Clipboard;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static net.hollowcube.terraform.nbt.NbtTypes.*;
import static org.junit.jupiter.api.Assertions.*;

/// Edge cases of the Sponge schematic reader and writer.
class SpongeSchematicEdgeCaseTest {
    private static final int DV = MinecraftServer.DATA_VERSION;

    static {
        MinecraftServer.init();
    }

    /// The v1/v2 spec does not name the root compound, and some writers leave it unnamed.
    @Test
    void v2WithAnUnnamedRootDecodes() throws IOException {
        byte[] schem = v2(""); // identical to the existing v2 fixture except for the root name
        Schematic c = SpongeSchematicReader.read(schem, GameDataProvider.NOOP);
        assertEquals(Block.STONE.stateId(), c.buffer().getBlockState(1, 0, 1));
    }

    /// Dimensions in 32768..65535 are written as the low 16 bits and read back unsigned; the data
    /// length and cell order survive.
    @Test
    void dimensionsAbove32767RoundTrip() throws IOException {
        int w = 40_000;
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(w - 1, 0, 0, Block.DIRT)
            .build();
        var encoded = SpongeSchematicWriter.write(
            new Schematic(buffer, BlockVec.ZERO),
            GameDataProvider.NOOP
        );
        CompoundBinaryTag schematic = schematic(encoded);
        assertEquals((short) w, schematic.getShort("Width"));
        assertEquals(w, schematic.getCompound("Blocks").getByteArray("Data").length);

        Schematic read = SpongeSchematicReader.read(encoded, GameDataProvider.NOOP);
        assertEquals(Block.STONE.stateId(), read.buffer().getBlockState(0, 0, 0));
        assertEquals(Block.DIRT.stateId(), read.buffer().getBlockState(w - 1, 0, 0));
        assertEquals(Block.AIR.stateId(), read.buffer().getBlockState(w / 2, 0, 0));
    }

    /// Thousands of distinct states: 1-, 2- and 3-byte varints, several VarIntWriter chunk
    /// flushes, and the precomputed byte-array length must agree with the bytes actually written
    /// (otherwise the NbtStreamWriter throws). Also pins the state-id ceiling the writer assumes.
    @Test
    void manyPaletteEntriesRoundTripAcrossVarintWidthsAndChunkFlushes() throws IOException {
        List<Block> states = new ArrayList<>();
        int maxStateId = 0;
        for (Block block : Block.values()) {
            for (Block state : block.possibleStates()) {
                maxStateId = Math.max(maxStateId, state.stateId());
                if (states.size() < 40_000) states.add(state);
            }
        }
        System.out.printf("block states: using %d, max state id %d%n", states.size(), maxStateId);
        assertTrue(
            maxStateId < 1 << 16,
            "the writer indexes paletteIndex[stateId] with a 2^16 table"
        );
        assertTrue(states.size() > 1 << 14, "need >16384 states to reach 3-byte varints");

        EditBuffer.Builder builder = EditBuffer.builder();
        for (int i = 0; i < states.size(); i++) builder.setBlockState(i, 0, 0, states.get(i));
        var encoded = SpongeSchematicWriter.write(
            new Schematic(builder.build(), BlockVec.ZERO),
            GameDataProvider.NOOP
        );
        Schematic read = SpongeSchematicReader.read(encoded, GameDataProvider.NOOP);
        for (int i = 0; i < states.size(); i++) {
            assertEquals(
                states.get(i).stateId(),
                read.buffer().getBlockState(i, 0, 0),
                "cell " + i
            );
        }
        CompoundBinaryTag schematic = schematic(encoded);
        assertEquals(states.size(), schematic.getCompound("Blocks").getCompound("Palette").size());
    }

    /// Block entity positions are written relative to the box min and read back through the same
    /// Offset as the blocks, for a box that does not start at the origin.
    @Test
    void blockEntityPositionsAgreeWithBlocksAwayFromTheOrigin() throws IOException {
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putInt("k", 9).build();
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(-20, 5, 33, Block.STONE)
            .setBlockState(-17, 7, 35, Block.CHEST.withNbt(nbt))
            .build();
        Clipboard clip = new Clipboard(buffer, new Vec(-3, 1, 40));
        var encoded = SpongeSchematicWriter.write(clip.toSchematic(), GameDataProvider.NOOP);
        CompoundBinaryTag schematic = schematic(encoded);
        assertArrayEquals(new int[] {-17, 4, -7}, schematic.getIntArray("Offset"));
        CompoundBinaryTag entry = schematic.getCompound("Blocks")
            .getList("BlockEntities")
            .getCompound(0);
        assertArrayEquals(new int[] {3, 2, 2}, entry.getIntArray("Pos"));

        Clipboard read = new Clipboard(SpongeSchematicReader.read(encoded, GameDataProvider.NOOP));
        // Pasted at the same anchor as the copy origin, the chest lands where it was.
        EditBuffer pasted = read.pasteBuffer(new Vec(-3, 1, 40), false);
        assertEquals(nbt, pasted.getBlock(-17, 7, 35).nbt());
    }

    /// A v3 schematic with no Offset tag at all (it is optional) still decodes, on the second pass.
    @Test
    void v3WithoutOffsetDecodes() throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(raw);
        o.writeByte(COMPOUND);
        o.writeUTF("");
        o.writeByte(COMPOUND);
        o.writeUTF("Schematic");
        intTag(o, "Version", 3);
        intTag(o, "DataVersion", DV);
        shortTag(o, "Width", 1);
        shortTag(o, "Height", 1);
        shortTag(o, "Length", 1);
        o.writeByte(COMPOUND);
        o.writeUTF("Blocks");
        o.writeByte(COMPOUND);
        o.writeUTF("Palette");
        intTag(o, "minecraft:stone", 0);
        o.writeByte(END);
        o.writeByte(BYTE_ARRAY);
        o.writeUTF("Data");
        o.writeInt(1);
        o.writeByte(0);
        o.writeByte(END);
        o.writeByte(END);
        o.writeByte(END);
        Schematic c = SpongeSchematicReader.read(gzip(raw), GameDataProvider.NOOP);
        assertEquals(Block.STONE.stateId(), c.buffer().getBlockState(0, 0, 0));
    }

    private static byte[] v2(String rootName) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(raw);
        o.writeByte(COMPOUND);
        o.writeUTF(rootName);
        intTag(o, "Version", 2);
        intTag(o, "DataVersion", DV);
        shortTag(o, "Width", 2);
        shortTag(o, "Height", 1);
        shortTag(o, "Length", 2);
        o.writeByte(COMPOUND);
        o.writeUTF("Palette");
        intTag(o, "minecraft:air", 0);
        intTag(o, "minecraft:stone", 1);
        o.writeByte(END);
        o.writeByte(BYTE_ARRAY);
        o.writeUTF("BlockData");
        o.writeInt(4);
        o.write(new byte[] {1, 0, 0, 1});
        o.writeByte(END);
        return gzip(raw);
    }

    private static void intTag(DataOutputStream o, String name, int v) throws IOException {
        o.writeByte(INT);
        o.writeUTF(name);
        o.writeInt(v);
    }

    private static void shortTag(DataOutputStream o, String name, int v) throws IOException {
        o.writeByte(SHORT);
        o.writeUTF(name);
        o.writeShort(v);
    }

    private static byte[] gzip(ByteArrayOutputStream raw) throws IOException {
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream z = new GZIPOutputStream(gz)) {
            raw.writeTo(z);
        }
        return gz.toByteArray();
    }

    private static CompoundBinaryTag schematic(byte[] data) throws IOException {
        return BinaryTagIO.unlimitedReader()
            .read(new ByteArrayInputStream(data), BinaryTagIO.Compression.GZIP)
            .getCompound("Schematic");
    }
}
