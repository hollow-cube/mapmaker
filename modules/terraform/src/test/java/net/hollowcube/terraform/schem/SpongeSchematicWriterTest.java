package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class SpongeSchematicWriterTest {
    static {
        MinecraftServer.init();
    }

    @Test
    void roundTripsBlocksAndWritesUnsetCellsAsAir() throws IOException {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(2, 1, 0, Block.DIRT)
            .setBlockState(1, 1, 0, Block.AIR)
            .build();
        Schematic read = roundTrip(new Schematic(buffer, BlockVec.ZERO));

        EditBuffer b = read.buffer();
        assertEquals(BlockVec.ZERO, read.origin());
        assertEquals(Block.STONE.stateId(), b.getBlockState(0, 0, 0));
        assertEquals(Block.DIRT.stateId(), b.getBlockState(2, 1, 0));
        assertEquals(Block.AIR.stateId(), b.getBlockState(1, 1, 0));
        assertEquals(Block.AIR.stateId(), b.getBlockState(1, 0, 0), "unset cell");
        assertEquals(Area.cuboid(new BlockVec(0, 0, 0), new BlockVec(2, 1, 0)), b.blockBounds());
        assertEquals(6, countSet(b));
    }

    @Test
    void originIsPreservedSoPastesMatch() throws IOException {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(12, 70, -3, Block.STONE)
            .setBlockState(14, 72, -1, Block.OAK_LOG.withProperty("axis", "z"))
            .build();
        Schematic clip = new Schematic(buffer, new Vec(10, 64, -5).asBlockVec());
        Schematic read = roundTrip(clip);

        assertSameRelativeCells(clip, read);
        BlockVec stone = read.origin().add(2, 6, 2);
        assertEquals(
            Block.STONE.stateId(),
            read.buffer().getBlockState(stone.blockX(), stone.blockY(), stone.blockZ())
        );
    }

    @Test
    void writesOffsetAndDimensionsRelativeToTheOrigin() throws IOException {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(12, 70, -3, Block.STONE)
            .setBlockState(14, 72, -1, Block.DIRT)
            .build();
        var encoded = SpongeSchematicWriter.write(
            new Schematic(buffer, new Vec(10, 64, -5).asBlockVec()),
            GameDataProvider.NOOP
        );
        CompoundBinaryTag schematic = schematic(encoded);

        assertEquals(3, schematic.getInt("Version"));
        assertEquals(3, schematic.getShort("Width"));
        assertEquals(3, schematic.getShort("Height"));
        assertEquals(3, schematic.getShort("Length"));
        assertArrayEquals(new int[] {2, 6, 2}, schematic.getIntArray("Offset"));
    }

    @Test
    void metadataNamesTheWriterAndKeepsTheOriginForWorldEdit() throws IOException {
        EditBuffer buffer = EditBuffer.builder().setBlockState(12, 70, -3, Block.STONE).build();
        long before = System.currentTimeMillis();
        var encoded = SpongeSchematicWriter.write(
            new Schematic(buffer, new Vec(10, 64, -5).asBlockVec()),
            GameDataProvider.NOOP
        );
        CompoundBinaryTag metadata = schematic(encoded).getCompound("Metadata");

        assertEquals("github.com/hollow-cube/terraform", metadata.getString("Writer"));
        assertTrue(metadata.getLong("Date") >= before);
        int[] origin = metadata.getCompound("WorldEdit").getIntArray("Origin");
        assertArrayEquals(new int[] {10, 64, -5}, origin);
        // WorldEdit places the area's minimum at Offset + Origin.
        int[] offset = schematic(encoded).getIntArray("Offset");
        assertArrayEquals(
            new int[] {12, 70, -3},
            new int[] {offset[0] + origin[0], offset[1] + origin[1], offset[2] + origin[2]}
        );
    }

    @Test
    void metadataSurvivesALoadAndSave() throws IOException {
        CompoundBinaryTag metadata = CompoundBinaryTag.builder()
            .putString("Name", "house")
            .putString("Author", "someone")
            .put("WorldEdit", CompoundBinaryTag.builder().putString("Version", "7.3").build())
            .build();
        EditBuffer buffer = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build();
        Schematic read = roundTrip(new Schematic(buffer, new BlockVec(4, 5, 6), metadata));

        assertEquals("house", read.metadata().getString("Name"));
        assertEquals("someone", read.metadata().getString("Author"));
        CompoundBinaryTag worldEdit = read.metadata().getCompound("WorldEdit");
        assertEquals("7.3", worldEdit.getString("Version"));
        assertArrayEquals(new int[] {4, 5, 6}, worldEdit.getIntArray("Origin"));
        assertEquals("github.com/hollow-cube/terraform", read.metadata().getString("Writer"));
    }

    @Test
    void stampsTheProvidersDataVersion() throws IOException {
        GameDataProvider provider = () -> 1234;
        EditBuffer buffer = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build();
        byte[] data = SpongeSchematicWriter.write(new Schematic(buffer, BlockVec.ZERO), provider);
        assertEquals(1234, schematic(data).getInt("DataVersion"));
    }

    @Test
    void blockEntitiesKeepTheirNbt() throws IOException {
        CompoundBinaryTag items = CompoundBinaryTag.builder().putString("Lock", "key").build();
        Block chest = Block.CHEST.withProperty("facing", "east").withNbt(items);
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(3, 5, 7, Block.STONE)
            .setBlockState(4, 5, 7, chest)
            .build();
        Schematic clip = new Schematic(buffer, new Vec(3, 5, 7).asBlockVec());
        var encoded = SpongeSchematicWriter.write(clip, GameDataProvider.NOOP);

        ListBinaryTag entities = schematic(encoded).getCompound("Blocks")
            .getList("BlockEntities", BinaryTagTypes.COMPOUND);
        assertEquals(1, entities.size());
        CompoundBinaryTag entry = entities.getCompound(0);
        assertEquals("minecraft:chest", entry.getString("Id"), "the vanilla block-entity type");
        assertArrayEquals(new int[] {1, 0, 0}, entry.getIntArray("Pos"));
        assertEquals(items, entry.getCompound("Data"));

        Block read = Objects.requireNonNull(
            SpongeSchematicReader.read(encoded, GameDataProvider.NOOP).buffer().getBlock(1, 0, 0)
        );
        assertEquals(chest.stateId(), read.stateId());
        assertEquals(items, read.nbt());
    }

    @Test
    void handlerKeyIsTheBlockEntityId() throws IOException {
        Key key = Key.key("test", "writer_marker");
        BlockHandler handler = () -> key;
        MinecraftServer.getBlockManager().registerHandler(key, () -> handler);
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putInt("Charge", 3).build();
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.CHEST.withHandler(handler).withNbt(nbt))
            .setBlockState(1, 0, 0, Block.STONE.withHandler(handler))
            .build();
        var encoded = SpongeSchematicWriter.write(
            new Schematic(buffer, BlockVec.ZERO),
            GameDataProvider.NOOP
        );

        ListBinaryTag entities = schematic(encoded).getCompound("Blocks")
            .getList("BlockEntities", BinaryTagTypes.COMPOUND);
        assertEquals(2, entities.size());
        for (int i = 0; i < entities.size(); i++) {
            assertEquals(key.asString(), entities.getCompound(i).getString("Id"));
        }

        EditBuffer read = SpongeSchematicReader.read(encoded, GameDataProvider.NOOP).buffer();
        Block chest = Objects.requireNonNull(read.getBlock(0, 0, 0));
        assertSame(handler, chest.handler());
        assertEquals(nbt, chest.nbt());
        Block stone = Objects.requireNonNull(read.getBlock(1, 0, 0));
        assertEquals(Block.STONE.stateId(), stone.stateId());
        assertSame(handler, stone.handler());
        assertNull(stone.nbt());
    }

    @Test
    void dataWithoutAHandlerOrBlockEntityTypeIsWrittenAsItsState() throws IOException {
        Block tagged = Block.STONE.withNbt(CompoundBinaryTag.builder().putInt("x", 1).build());
        EditBuffer buffer = EditBuffer.builder().setBlockState(0, 0, 0, tagged).build();
        var encoded = SpongeSchematicWriter.write(
            new Schematic(buffer, BlockVec.ZERO),
            GameDataProvider.NOOP
        );

        assertEquals(0, schematic(encoded).getCompound("Blocks").getList("BlockEntities").size());
        assertEquals(
            Block.STONE,
            SpongeSchematicReader.read(encoded, GameDataProvider.NOOP).buffer().getBlock(0, 0, 0)
        );
    }

    @Test
    void manyStatesUseMultiByteVarints() throws IOException {
        int states = 300;
        EditBuffer.Builder builder = EditBuffer.builder();
        for (int i = 0; i < states; i++) builder.setBlockState(i - 150, 0, 0, i + 1);
        Schematic read = roundTrip(new Schematic(builder.build(), BlockVec.ZERO));

        for (int i = 0; i < states; i++) {
            assertEquals(i + 1, read.buffer().getBlockState(i - 150, 0, 0), "cell " + i);
        }
    }

    @Test
    void randomSparseCellsRoundTripAcrossSections() throws IOException {
        int[] palette = {
            Block.STONE.stateId(),
            Block.AIR.stateId(),
            Block.GLASS.stateId(),
            Block.OAK_STAIRS.withProperty("facing", "west").stateId(),
            Block.REDSTONE_WIRE.withProperty("power", "7").stateId(),
        };
        var random = new SplittableRandom(7);
        EditBuffer.Builder builder = EditBuffer.builder();
        for (int i = 0; i < 2000; i++) {
            builder.setBlockState(
                random.nextInt(-40, 40),
                random.nextInt(-20, 20),
                random.nextInt(-40, 40),
                palette[random.nextInt(palette.length)]
            );
        }
        Schematic clip = new Schematic(builder.build(), new Vec(3, -7, 11).asBlockVec());
        assertSameRelativeCells(clip, roundTrip(clip));
    }

    @Test
    void filledSectionRoundTrips() throws IOException {
        EditBuffer buffer = EditBuffer.builder()
            .fillSection(-1, 0, 0, Block.STONE.stateId())
            .setBlockState(3, 20, 3, Block.DIRT)
            .build();
        Schematic clip = new Schematic(buffer, new Vec(-1, 0, 0).asBlockVec());
        Schematic read = roundTrip(clip);
        assertSameRelativeCells(clip, read);
        assertEquals(20 * 21 * 16, countSet(read.buffer()));
    }

    @Test
    void schematicWithoutBlocksIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SpongeSchematicWriter.write(
                new Schematic(EditBuffer.empty(), BlockVec.ZERO),
                GameDataProvider.NOOP
            )
        );
        EditBuffer biomesOnly = EditBuffer.builder().setBiome(0, 0, 0, 1).build();
        assertThrows(
            IllegalArgumentException.class,
            () -> SpongeSchematicWriter.write(
                new Schematic(biomesOnly, BlockVec.ZERO),
                GameDataProvider.NOOP
            )
        );
    }

    @Test
    void sparseSchematicWithTooManyCellsIsRejected() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(60_000, 1_000, 60_000, Block.STONE)
            .build();
        assertThrows(
            IllegalArgumentException.class,
            () -> SpongeSchematicWriter.write(
                new Schematic(buffer, BlockVec.ZERO),
                GameDataProvider.NOOP
            )
        );
    }

    @Test
    void oversizedSchematicIsRejected() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(70_000, 0, 0, Block.STONE)
            .build();
        assertThrows(
            IllegalArgumentException.class,
            () -> SpongeSchematicWriter.write(
                new Schematic(buffer, BlockVec.ZERO),
                GameDataProvider.NOOP
            )
        );
    }

    private static Schematic roundTrip(Schematic schematic) throws IOException {
        return SpongeSchematicReader.read(
            SpongeSchematicWriter.write(schematic, GameDataProvider.NOOP),
            GameDataProvider.NOOP
        );
    }

    /// Every cell `expected` sets, `actual` holds at the same position relative to its origin. `actual`
    /// may add air for the cells `expected` left unset.
    private static void assertSameRelativeCells(Schematic expected, Schematic actual) {
        BlockVec shift = actual.origin().sub(expected.origin());
        EditBuffer want = expected.buffer();
        EditBuffer got = actual.buffer();
        want.forEachSection(
            (sx, sy, sz, section) -> section.forEachBlockState((x, y, z, stateId) -> {
                int ax = (sx << 4) + x, ay = (sy << 4) + y, az = (sz << 4) + z;
                int bx = ax + shift.blockX(), by = ay + shift.blockY(), bz = az + shift.blockZ();
                assertEquals(stateId, got.getBlockState(bx, by, bz), ax + ", " + ay + ", " + az);
                assertEquals(want.getBlock(ax, ay, az), got.getBlock(bx, by, bz));
            })
        );
        got.forEachSection(
            (sx, sy, sz, section) -> section.forEachBlockState((x, y, z, stateId) -> {
                int bx = (sx << 4) + x, by = (sy << 4) + y, bz = (sz << 4) + z;
                int ax = bx - shift.blockX(), ay = by - shift.blockY(), az = bz - shift.blockZ();
                if (want.getBlockState(ax, ay, az) == EditBuffer.NONE) {
                    assertEquals(Block.AIR.stateId(), stateId, bx + ", " + by + ", " + bz);
                }
            })
        );
    }

    private static CompoundBinaryTag schematic(byte[] data) throws IOException {
        return BinaryTagIO.reader()
            .read(new ByteArrayInputStream(data), BinaryTagIO.Compression.GZIP)
            .getCompound("Schematic");
    }

    private static long countSet(EditBuffer buffer) {
        long[] n = {0};
        buffer.forEachSection((_, _, _, section) -> n[0] += section.count());
        return n[0];
    }
}
