package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/// Fixtures are hand-encoded raw NBT ([NbtOut]) so tag order is deterministic, since whether the
/// block data decodes in one pass or two keys off that order.
class SpongeSchematicReaderTest {
    private static final int DV = MinecraftServer.DATA_VERSION;

    private static final Block STONE = Block.STONE;
    private static final Block CHEST = Block.CHEST.withProperty("facing", "north");

    static {
        MinecraftServer.init();
    }

    @Test
    void readsV3WithBlockEntity() throws IOException {
        byte[] schem = v3(true);
        Schematic c = read(schem, GameDataProvider.NOOP);

        assertEquals(BlockVec.ZERO, c.origin());
        EditBuffer b = c.buffer();
        // The Offset (1, 64, 2) is baked into the cells.
        assertEquals(STONE.stateId(), b.getBlockState(1, 64, 2));
        assertEquals(CHEST.stateId(), b.getBlockState(2, 64, 2));
        assertEquals(Block.AIR.stateId(), b.getBlockState(2, 65, 2));
        assertEquals(EditBuffer.NONE, b.getBlockState(0, 64, 2), "outside the box");

        Block blockEntity = Objects.requireNonNull(b.getBlock(2, 64, 2));
        assertEquals(CHEST.stateId(), blockEntity.stateId());
        assertEquals(42, nbt(blockEntity).getInt("test"));
    }

    @Test
    void v3DecodesInASecondPassWhenDataPrecedesPalette() throws IOException {
        byte[] schem = v3(false);
        Schematic c = read(schem, GameDataProvider.NOOP);

        EditBuffer b = c.buffer();
        assertEquals(STONE.stateId(), b.getBlockState(1, 64, 2));
        assertEquals(CHEST.stateId(), b.getBlockState(2, 64, 2));
        assertEquals(42, nbt(Objects.requireNonNull(b.getBlock(2, 64, 2))).getInt("test"));
    }

    @Test
    void readsV2WithWeOffsetOverride() throws IOException {
        // Below v3 the paste-relative WEOffset wins over the Offset tag.
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 2).s("Height", 1).s("Length", 2);
        n.ints("Offset", 1, 1, 1);
        n.beginCompound("Metadata").i("WEOffsetX", 10).i("WEOffsetY", 0).i("WEOffsetZ", 10).end();
        n.beginCompound("Palette").i("minecraft:air", 0).i("minecraft:stone", 1).end();
        n.i("PaletteMax", 2);
        n.bytes("BlockData", varints(1, 0, 0, 1));
        n.end();
        Schematic c = read(n.gzip(), GameDataProvider.NOOP);

        EditBuffer b = c.buffer();
        assertEquals(STONE.stateId(), b.getBlockState(10, 0, 10));
        assertEquals(STONE.stateId(), b.getBlockState(11, 0, 11));
        assertEquals(Block.AIR.stateId(), b.getBlockState(11, 0, 10));
    }

    @Test
    void readsV1WithTileEntities() throws IOException {
        Block chestSouth = Block.CHEST.withProperty("facing", "south");
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 1).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:chest[facing=south]", 0).end();
        n.i("PaletteMax", 1);
        n.bytes("BlockData", varints(0));
        // Inline fields, no Data wrapper.
        n.beginListOfCompound("TileEntities", 1);
        n.str("Id", "minecraft:chest")
            .ints("Pos", 0, 0, 0)
            .i("ContentVersion", 1)
            .i("test", 7)
            .end();
        n.end();
        Schematic c = read(n.gzip(), GameDataProvider.NOOP);

        EditBuffer b = c.buffer();
        assertEquals(chestSouth.stateId(), b.getBlockState(0, 0, 0));
        CompoundBinaryTag data = nbt(Objects.requireNonNull(b.getBlock(0, 0, 0)));
        assertEquals(7, data.getInt("test"));
        assertNull(data.get("ContentVersion"));
        assertNull(data.get("Id"));
        assertNull(data.get("Pos"));
    }

    @Test
    void appliesGameDataUpgradeHook() throws IOException {
        GameDataProvider provider = new GameDataProvider() {
            @Override
            public int dataVersion() {
                return Integer.MAX_VALUE;
            }

            @Override
            public String upgradeBlockState(int from, int to, String state) {
                int bracket = state.indexOf('[');
                String id = bracket < 0 ? state : state.substring(0, bracket);
                return "minecraft:grass".equals(id) ? "minecraft:short_grass" : state;
            }
        };

        NbtOut n = new NbtOut();
        n.beginCompound("");
        n.beginCompound("Schematic");
        n.i("Version", 3).i("DataVersion", 1);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Blocks");
        n.beginCompound("Palette").i("minecraft:grass", 0).end();
        n.bytes("Data", varints(0));
        n.end();
        n.end();
        n.end();
        Schematic c = read(n.gzip(), provider);

        assertEquals(Block.SHORT_GRASS.stateId(), c.buffer().getBlockState(0, 0, 0));
    }

    @Test
    void blockEntityUpgradeCanRenameId() throws IOException {
        // The handler resolves by the returned type, not the stored one.
        Key renamed = Key.key("test", "renamed_chest");
        BlockHandler handler = () -> renamed;
        MinecraftServer.getBlockManager().registerHandler(renamed, () -> handler);
        GameDataProvider provider = new GameDataProvider() {
            @Override
            public int dataVersion() {
                return Integer.MAX_VALUE;
            }

            @Override
            public TypedData upgradeBlockEntity(int from, int to, TypedData entity) {
                if (!"minecraft:old_chest".equals(entity.type())) return entity;
                return new TypedData(
                    renamed.asString(),
                    CompoundBinaryTag.builder().put(entity.nbt()).putInt("upgraded", 1).build()
                );
            }
        };

        NbtOut n = new NbtOut();
        n.beginCompound("");
        n.beginCompound("Schematic");
        n.i("Version", 3).i("DataVersion", 1);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Blocks");
        n.beginCompound("Palette").i("minecraft:chest[facing=north]", 0).end();
        n.bytes("Data", varints(0));
        n.beginListOfCompound("BlockEntities", 1);
        n.str("Id", "minecraft:old_chest").ints("Pos", 0, 0, 0);
        n.beginCompound("Data").i("test", 42).end();
        n.end();
        n.end();
        n.end();
        n.end();
        Schematic c = read(n.gzip(), provider);

        Block blockEntity = Objects.requireNonNull(c.buffer().getBlock(0, 0, 0));
        assertSame(handler, blockEntity.handler());
        assertEquals(1, nbt(blockEntity).getInt("upgraded"));
        assertEquals(42, nbt(blockEntity).getInt("test"));
    }

    @Test
    void legacyCapitalizedBlockEntityIdResolvesItsHandler() throws IOException {
        Key key = Key.key("test", "legacy_beacon");
        BlockHandler handler = () -> key;
        MinecraftServer.getBlockManager().registerHandler(key, () -> handler);
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:beacon", 0).end();
        n.bytes("BlockData", varints(0));
        n.beginListOfCompound("BlockEntities", 1);
        n.str("Id", "test:Legacy_Beacon").ints("Pos", 0, 0, 0).end();
        n.end();
        Block blockEntity = Objects.requireNonNull(
            read(n.gzip(), GameDataProvider.NOOP).buffer().getBlock(0, 0, 0)
        );
        assertSame(handler, blockEntity.handler());
        assertNull(blockEntity.nbt(), "an empty payload attaches no NBT");
    }

    @Test
    void blockEntityOutsideTheBoxIsIgnored() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.bytes("BlockData", varints(0));
        n.beginListOfCompound("BlockEntities", 1);
        n.str("Id", "minecraft:chest").ints("Pos", 5, 0, 0).i("test", 1).end();
        n.end();
        EditBuffer b = read(n.gzip(), GameDataProvider.NOOP).buffer();
        assertEquals(STONE, b.getBlock(0, 0, 0));
        assertEquals(EditBuffer.NONE, b.getBlockState(5, 0, 0));
    }

    @Test
    void readsMultiByteVarints() throws IOException {
        int states = 300;
        int[] ids = new int[states];
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", states).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette");
        for (int i = 0; i < states; i++) {
            n.i(Objects.requireNonNull(Block.fromStateId(i + 1)).state(), i);
            ids[i] = i;
        }
        n.end();
        n.bytes("BlockData", varints(ids));
        n.end();
        EditBuffer b = read(n.gzip(), GameDataProvider.NOOP).buffer();
        for (int i = 0; i < states; i++) assertEquals(i + 1, b.getBlockState(i, 0, 0));
    }

    @Test
    void rejectsAnUnsupportedVersion() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 4);
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void rejectsAMissingSchematicCompound() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("");
        n.i("Version", 3);
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void rejectsMissingBlockData() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void rejectsBlockDataThatDisagreesWithTheDimensions() throws IOException {
        for (int cells : new int[] {3, 5}) {
            NbtOut n = new NbtOut();
            n.beginCompound("Schematic");
            n.i("Version", 2).i("DataVersion", DV);
            n.s("Width", 2).s("Height", 2).s("Length", 1);
            n.ints("Offset", 0, 0, 0);
            n.beginCompound("Metadata").end();
            n.beginCompound("Palette").i("minecraft:stone", 0).end();
            n.bytes("BlockData", varints(new int[cells]));
            n.end();
            byte[] schem = n.gzip();
            assertThrows(
                IOException.class,
                () -> read(schem, GameDataProvider.NOOP),
                cells + " cells"
            );
        }
    }

    @Test
    void rejectsAPaletteIdPastThePalette() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.bytes("BlockData", varints(1));
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void decodesHashOrderedWriterInASecondPass() throws IOException {
        // A hash-ordered writer puts Blocks before Version, DataVersion and the dimensions.
        NbtOut n = new NbtOut();
        n.beginCompound("");
        n.beginCompound("Schematic");
        n.beginCompound("Blocks");
        blocksPalette(n);
        n.bytes("Data", varints(1, 2, 0, 0, 0, 0, 0, 0));
        n.end();
        n.i("Version", 3);
        n.i("DataVersion", DV);
        n.s("Width", 2).s("Height", 2).s("Length", 2);
        n.ints("Offset", 0, 0, 0);
        n.end();
        n.end();
        Schematic c = read(n.gzip(), GameDataProvider.NOOP);
        assertEquals(STONE.stateId(), c.buffer().getBlockState(0, 0, 0));
        assertEquals(CHEST.stateId(), c.buffer().getBlockState(1, 0, 0));
        assertEquals(Block.AIR.stateId(), c.buffer().getBlockState(1, 1, 1));
    }

    @Test
    void paletteResolvesAgainstDataVersionReadAfterIt() throws IOException {
        GameDataProvider provider = new GameDataProvider() {
            @Override
            public int dataVersion() {
                return DV;
            }

            @Override
            public String upgradeBlockState(int from, int to, String state) {
                throw new AssertionError("upgrade hook ran for a current-version schematic");
            }
        };
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.bytes("BlockData", varints(0));
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.end();
        assertEquals(STONE.stateId(), read(n.gzip(), provider).buffer().getBlockState(0, 0, 0));
    }

    @Test
    void unknownBlockStateResolvesToAir() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 2).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette")
            .i("minecraft:not_a_block", 0)
            .i("minecraft:stone[facing=nowhere]", 1)
            .end();
        n.bytes("BlockData", varints(0, 1));
        n.end();
        EditBuffer b = read(n.gzip(), GameDataProvider.NOOP).buffer();
        assertEquals(Block.AIR.stateId(), b.getBlockState(0, 0, 0), "unknown key");
        assertEquals(Block.AIR.stateId(), b.getBlockState(1, 0, 0), "invalid property");
    }

    @Test
    void paletteGapsResolveToAir() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 2).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:stone", 2).end();
        n.bytes("BlockData", varints(0, 2));
        n.end();
        EditBuffer b = read(n.gzip(), GameDataProvider.NOOP).buffer();
        assertEquals(Block.AIR.stateId(), b.getBlockState(0, 0, 0));
        assertEquals(STONE.stateId(), b.getBlockState(1, 0, 0));
    }

    @Test
    void dimensionsAreUnsignedShorts() throws IOException {
        int width = 40_000;
        int[] ids = new int[width];
        ids[width - 1] = 1;
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", width).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:air", 0).i("minecraft:stone", 1).end();
        n.bytes("BlockData", varints(ids));
        n.end();
        EditBuffer b = read(n.gzip(), GameDataProvider.NOOP).buffer();
        assertEquals(STONE.stateId(), b.getBlockState(width - 1, 0, 0));
    }

    @Test
    void overflowingDimensionsAreRejected() throws IOException {
        // The cell count overflows a long, so a wrapped count must never admit the read.
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.i("Width", Integer.MAX_VALUE)
            .i("Height", Integer.MAX_VALUE)
            .i("Length", Integer.MAX_VALUE);
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.bytes("BlockData", varints(0));
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void hostileBlockDataLengthIsNeverAllocated() throws IOException {
        // Streamed (palette first) or deferred (data first), the declared length is never allocated.
        for (boolean paletteFirst : new boolean[] {true, false}) {
            NbtOut n = new NbtOut();
            n.beginCompound("Schematic");
            n.i("Version", 2).i("DataVersion", DV);
            n.s("Width", 1).s("Height", 1).s("Length", 1);
            n.ints("Offset", 0, 0, 0);
            n.beginCompound("Metadata").end();
            if (paletteFirst) n.beginCompound("Palette").i("minecraft:stone", 0).end();
            n.header(7, "BlockData");
            n.o.writeInt(1 << 30);
            n.o.writeByte(0);
            if (!paletteFirst) n.beginCompound("Palette").i("minecraft:stone", 0).end();
            n.end();
            byte[] schem = n.gzip();
            assertThrows(
                IOException.class,
                () -> read(schem, GameDataProvider.NOOP),
                "paletteFirst=" + paletteFirst
            );
        }
    }

    @Test
    void hostileBlockEntityArrayLengthIsRejected() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:chest", 0).end();
        n.bytes("BlockData", varints(0));
        n.beginListOfCompound("BlockEntities", 1);
        n.str("Id", "minecraft:chest").ints("Pos", 0, 0, 0);
        n.header(11, "Items");
        n.o.writeInt(1 << 30);
        n.end();
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void blockEntityListOfEndElementsIsRejected() throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.bytes("BlockData", varints(0));
        n.header(9, "BlockEntities");
        n.o.writeByte(0);
        n.o.writeInt(Integer.MAX_VALUE);
        n.end();
        byte[] schem = n.gzip();
        assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
    }

    @Test
    void paletteNamesCountTowardTheValueLimit() throws IOException {
        String filler = "a".repeat(65_000);
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette");
        for (int i = 0; i < 1100; i++) n.i(i + filler, i);
        n.end();
        n.bytes("BlockData", varints(0));
        n.end();
        byte[] schem = n.gzip();
        var e = assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
        assertTrue(e.getMessage().contains("NBT values exceed"), e.getMessage());
    }

    @Test
    void blockEntityIdsCountTowardTheValueLimit() throws IOException {
        String id = "minecraft:" + "a".repeat(65_000);
        NbtOut n = new NbtOut();
        n.beginCompound("Schematic");
        n.i("Version", 2).i("DataVersion", DV);
        n.s("Width", 1).s("Height", 1).s("Length", 1);
        n.beginCompound("Palette").i("minecraft:stone", 0).end();
        n.bytes("BlockData", varints(0));
        n.beginListOfCompound("BlockEntities", 1100);
        for (int i = 0; i < 1100; i++) n.str("Id", id).ints("Pos", 0, 0, 0).end();
        n.end();
        byte[] schem = n.gzip();
        var e = assertThrows(IOException.class, () -> read(schem, GameDataProvider.NOOP));
        assertTrue(e.getMessage().contains("NBT values exceed"), e.getMessage());
    }

    @Test
    void oversizedPaletteIsRejected() throws IOException {
        NbtOut many = new NbtOut();
        many.beginCompound("Schematic");
        many.beginCompound("Palette");
        for (int i = 0; i < 70_000; i++) many.i("minecraft:stone" + i, 0);
        many.end();
        many.end();
        byte[] manyEntries = many.gzip();
        var tooMany = assertThrows(
            IOException.class,
            () -> read(manyEntries, GameDataProvider.NOOP)
        );
        assertTrue(tooMany.getMessage().contains("entries"), tooMany.getMessage());

        NbtOut high = new NbtOut();
        high.beginCompound("Schematic");
        high.beginCompound("Palette").i("minecraft:stone", 1 << 16).end();
        high.end();
        byte[] highId = high.gzip();
        var e = assertThrows(IOException.class, () -> read(highId, GameDataProvider.NOOP));
        assertTrue(e.getMessage().contains("palette id"), e.getMessage());
    }

    private static Schematic read(byte[] schem, GameDataProvider provider) throws IOException {
        return SpongeSchematicReader.read(schem, provider);
    }

    /// A 2x2x2 v3 schematic at offset (1, 64, 2): stone at (0, 0, 0), a chest block entity at
    /// (1, 0, 0), the rest air.
    private static byte[] v3(boolean paletteFirst) throws IOException {
        NbtOut n = new NbtOut();
        n.beginCompound("");
        n.beginCompound("Schematic");
        n.i("Version", 3).i("DataVersion", DV);
        n.s("Width", 2).s("Height", 2).s("Length", 2);
        n.ints("Offset", 1, 64, 2);
        n.beginCompound("Blocks");
        if (paletteFirst) {
            blocksPalette(n);
            n.bytes("Data", varints(1, 2, 0, 0, 0, 0, 0, 0));
        } else {
            n.bytes("Data", varints(1, 2, 0, 0, 0, 0, 0, 0));
            blocksPalette(n);
        }
        n.beginListOfCompound("BlockEntities", 1);
        n.str("Id", "minecraft:chest").ints("Pos", 1, 0, 0);
        n.beginCompound("Data").i("test", 42).end();
        n.end();
        n.end();
        n.end();
        n.end();
        return n.gzip();
    }

    private static void blocksPalette(NbtOut n) throws IOException {
        n.beginCompound("Palette")
            .i("minecraft:air", 0)
            .i("minecraft:stone", 1)
            .i("minecraft:chest[facing=north]", 2)
            .end();
    }

    private static CompoundBinaryTag nbt(Block block) {
        return Objects.requireNonNull(block.nbt());
    }

    /// Encodes ids as a varint byte array in the schematic's YZX order.
    private static byte[] varints(int... ids) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int id : ids) {
            while ((id & ~0x7F) != 0) {
                b.write((id & 0x7F) | 0x80);
                id >>>= 7;
            }
            b.write(id);
        }
        return b.toByteArray();
    }

    /// Raw NBT in the order written, gzipped.
    private static final class NbtOut {
        private final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream o = new DataOutputStream(raw);

        NbtOut header(int type, String name) throws IOException {
            o.writeByte(type);
            o.writeUTF(name);
            return this;
        }

        NbtOut i(String name, int v) throws IOException {
            header(3, name);
            o.writeInt(v);
            return this;
        }

        NbtOut s(String name, int v) throws IOException {
            header(2, name);
            o.writeShort(v);
            return this;
        }

        NbtOut str(String name, String v) throws IOException {
            header(8, name);
            o.writeUTF(v);
            return this;
        }

        NbtOut ints(String name, int... a) throws IOException {
            header(11, name);
            o.writeInt(a.length);
            for (int x : a) o.writeInt(x);
            return this;
        }

        NbtOut bytes(String name, byte[] data) throws IOException {
            header(7, name);
            o.writeInt(data.length);
            o.write(data);
            return this;
        }

        NbtOut beginCompound(String name) throws IOException {
            return header(10, name);
        }

        NbtOut beginListOfCompound(String name, int len) throws IOException {
            header(9, name);
            o.writeByte(10);
            o.writeInt(len);
            return this;
        }

        NbtOut end() throws IOException {
            o.writeByte(0);
            return this;
        }

        byte[] gzip() throws IOException {
            o.flush();
            ByteArrayOutputStream g = new ByteArrayOutputStream();
            try (GZIPOutputStream z = new GZIPOutputStream(g)) {
                z.write(raw.toByteArray());
            }
            return g.toByteArray();
        }
    }
}
