package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.util.GameDataProvider;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpongeSchematicEntityTest {
    private static final String ZOMBIE = "minecraft:zombie";

    static {
        MinecraftServer.init();
    }

    @Test
    void writtenEntitiesReadBack() throws IOException {
        UUID id = UUID.randomUUID();
        var pos = new Pos(1.5, 64.25, 2.5, 45, 10);
        var buffer = EditBuffer.builder()
            .setBlockState(0, 64, 0, Block.STONE)
            .setBlockState(3, 66, 3, Block.STONE)
            .addEntityEdit(new EntityEdit(id, pos, new TypedData(ZOMBIE, named("bob"))))
            .addEntityEdit(EntityEdit.remove(UUID.randomUUID(), pos))
            .build();

        var bytes = SpongeSchematicWriter.write(
            new Schematic(buffer, new BlockVec(1, 64, 1)),
            GameDataProvider.NOOP
        );
        var read = SpongeSchematicReader.read(bytes, GameDataProvider.NOOP);

        assertEquals(1, read.buffer().entities().size(), "removals are not saved");
        var entity = read.buffer().entities().getFirst();
        assertNotEquals(id, entity.id(), "ids aren't saved");
        assertNull(entity.data().nbt().get("UUID"));
        assertEquals(ZOMBIE, entity.data().type());
        assertEquals(
            pos.sub(1, 64, 1),
            entity.pos(),
            "relative to the origin, like the blocks"
        );
        assertEquals("bob", entity.data().nbt().getString("CustomName"));
    }

    @Test
    void readsV2InlineEntities() throws IOException {
        var entity = CompoundBinaryTag.builder()
            .putString("Id", "zombie")
            .put("Pos", doubles(0.5, 0, 0.5))
            .put("Rotation", floats(90, 0))
            .putString("CustomName", "bob")
            .build();
        var schematic = CompoundBinaryTag.builder()
            .putInt("Version", 2)
            .putInt("DataVersion", MinecraftServer.DATA_VERSION)
            .putShort("Width", (short) 1)
            .putShort("Height", (short) 1)
            .putShort("Length", (short) 1)
            .putIntArray("Offset", new int[] {10, 0, 10})
            .put("Palette", CompoundBinaryTag.builder().putInt("minecraft:stone", 0).build())
            .putByteArray("BlockData", new byte[] {0})
            .put("Entities", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, List.of(entity)))
            .build();

        var read = SpongeSchematicReader.read(gzip("Schematic", schematic), GameDataProvider.NOOP);

        var loaded = read.buffer().entities().getFirst();
        var data = loaded.data();
        assertEquals(ZOMBIE, data.type());
        assertEquals(new Pos(10.5, 0, 10.5, 90, 0), loaded.pos());
        assertEquals("bob", data.nbt().getString("CustomName"));
    }

    @Test
    void olderEntitiesGoThroughTheUpgradeHook() throws IOException {
        var provider = new GameDataProvider() {
            @Override
            public int dataVersion() {
                return Integer.MAX_VALUE;
            }

            @Override
            public TypedData upgradeEntity(int from, int to, TypedData entity) {
                return new TypedData("minecraft:husk", entity.nbt().putBoolean("upgraded", true));
            }
        };
        var buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .addEntityEdit(
                new EntityEdit(
                    UUID.randomUUID(),
                    Pos.ZERO,
                    new TypedData(ZOMBIE, named("x"))
                )
            )
            .build();
        var bytes = SpongeSchematicWriter.write(
            new Schematic(buffer, BlockVec.ZERO),
            GameDataProvider.NOOP
        );

        var data = SpongeSchematicReader.read(bytes, provider)
            .buffer()
            .entities()
            .getFirst()
            .data();

        assertEquals("minecraft:husk", data.type());
        assertTrue(data.nbt().getBoolean("upgraded"));
    }

    private static CompoundBinaryTag named(String name) {
        return CompoundBinaryTag.builder().putString("CustomName", name).build();
    }

    private static ListBinaryTag doubles(double... values) {
        var builder = ListBinaryTag.builder(BinaryTagTypes.DOUBLE);
        for (double value : values) builder.add(DoubleBinaryTag.doubleBinaryTag(value));
        return builder.build();
    }

    private static ListBinaryTag floats(float... values) {
        var builder = ListBinaryTag.builder(BinaryTagTypes.FLOAT);
        for (float value : values) builder.add(FloatBinaryTag.floatBinaryTag(value));
        return builder.build();
    }

    private static byte[] gzip(String rootName, CompoundBinaryTag root) throws IOException {
        var out = new ByteArrayOutputStream();
        BinaryTagIO.writer().writeNamed(
            Map.entry(rootName, root),
            out,
            BinaryTagIO.Compression.GZIP
        );
        return out.toByteArray();
    }
}
