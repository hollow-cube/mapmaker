package net.hollowcube.terraform.buffer;

import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.util.BlockUtil;
import net.hollowcube.terraform.util.GameDataProvider;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.biome.Biome;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EditBufferSerializerTest {
    private static final Block[] MIX = {
        Block.STONE,
        Block.DIRT,
        Block.OAK_STAIRS.withProperty("facing", "east"),
        Block.AIR,
    };

    static {
        MinecraftServer.init();
    }

    @Test
    void roundTripsEveryChannel() {
        var chest = Block.CHEST.withNbt(
            CompoundBinaryTag.builder().putString("CustomName", "loot").build()
        );
        var builder = EditBuffer.builder()
            .setBlockState(-3, 70, 5, Block.GLASS) // sparse
            .setBlockState(-2, 70, 5, chest)
            .fill(
                Area.cuboid(new BlockVec(32, 0, 32), new BlockVec(47, 15, 47)),
                Block.SAND.stateId()
            ) // uniform
            .setBiome(-3, 70, 5, biome("minecraft:desert"))
            .setBiome(40, 4, 40, biome("minecraft:forest"))
            .addEntityEdit(
                new EntityEdit(
                    UUID.randomUUID(),
                    new Pos(1.5, 70, 2.25, 45, 10),
                    new TypedData(
                        "minecraft:zombie",
                        CompoundBinaryTag.builder().putString("tag", "x").build()
                    )
                )
            )
            .addEntityEdit(EntityEdit.remove(UUID.randomUUID(), new Pos(3, 4, 5)));
        var random = new Random(7);
        for (int i = 0; i < 2000; i++) { // dense, a palette of several states
            builder.setBlockState(
                64 + random.nextInt(16),
                random.nextInt(16),
                random.nextInt(16),
                MIX[random.nextInt(MIX.length)]
            );
        }
        var buffer = builder.build();

        var read = roundTrip(buffer, GameDataProvider.NOOP, GameDataProvider.NOOP);

        assertSameContent(buffer, read);
        assertEquals(buffer.blockBounds(), read.blockBounds());
        assertEquals(buffer.entities(), read.entities());
        assertEquals(chest, read.getBlock(-2, 70, 5));
    }

    @Test
    void eachNameIsWrittenOnce() {
        var buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(100, 0, 0, Block.STONE)
            .fill(
                Area.cuboid(new BlockVec(0, 32, 0), new BlockVec(15, 47, 15)),
                Block.STONE.stateId()
            )
            .build();

        var type = EditBuffer.serializer(GameDataProvider.NOOP, MinecraftServer.getBiomeRegistry());
        var text = new String(
            NetworkBuffer.makeArray(out -> out.write(type, buffer)),
            StandardCharsets.ISO_8859_1
        );

        assertEquals(
            text.indexOf("minecraft:stone"),
            text.lastIndexOf("minecraft:stone"),
            "three sections, one name"
        );
        assertSameContent(buffer, roundTrip(buffer, GameDataProvider.NOOP, GameDataProvider.NOOP));
    }

    @Test
    void anEmptyBufferRoundTrips() {
        var read = roundTrip(EditBuffer.empty(), GameDataProvider.NOOP, GameDataProvider.NOOP);

        assertEquals(0, read.sectionCount());
        assertNull(read.blockBounds());
        assertTrue(read.entities().isEmpty());
    }

    @Test
    void olderBlockStatesGoThroughTheUpgrade() {
        var buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .fill(
                Area.cuboid(new BlockVec(16, 0, 0), new BlockVec(31, 15, 15)),
                Block.STONE.stateId()
            )
            .build();
        var upgrade = new Newer() {
            @Override
            public String upgradeBlockState(int from, int to, String state) {
                return state.equals("minecraft:stone") ? "minecraft:granite" : state;
            }
        };

        var read = roundTrip(buffer, GameDataProvider.NOOP, upgrade);

        assertEquals(Block.GRANITE.stateId(), read.getBlockState(0, 0, 0));
        assertEquals(Block.GRANITE.stateId(), read.getBlockState(20, 5, 5));
    }

    @Test
    void unknownBlockStatesReadAsAir() {
        // A dense section whose two states both resolve to air, which a palette can't hold twice
        var builder = EditBuffer.builder();
        for (int i = 0; i < 300; i++)
            builder.setBlockState(
                i & 15,
                i >> 4 & 15,
                i >> 8,
                i % 2 == 0 ? Block.STONE : Block.DIRT
            );
        var buffer = builder.build();
        var upgrade = new Newer() {
            @Override
            public String upgradeBlockState(int from, int to, String state) {
                return "minecraft:gone_" + state.substring("minecraft:".length());
            }
        };

        var read = roundTrip(buffer, GameDataProvider.NOOP, upgrade);

        for (int i = 0; i < 300; i++)
            assertEquals(Block.AIR.stateId(), read.getBlockState(i & 15, i >> 4 & 15, i >> 8));
        assertEquals(EditBuffer.NONE, read.getBlockState(15, 15, 15), "cells never set stay unset");
    }

    @Test
    void olderEntitiesGoThroughTheUpgradeAndInvalidOnesAreDropped() {
        var buffer = EditBuffer.builder()
            .addEntityEdit(
                new EntityEdit(
                    UUID.randomUUID(),
                    Pos.ZERO,
                    new TypedData("minecraft:zombie", CompoundBinaryTag.empty())
                )
            )
            .addEntityEdit(
                new EntityEdit(
                    UUID.randomUUID(),
                    Pos.ZERO,
                    new TypedData("minecraft:old", CompoundBinaryTag.empty())
                )
            )
            .build();
        var upgrade = new Newer() {
            @Override
            public TypedData upgradeEntity(int from, int to, TypedData entity) {
                return new TypedData(
                    entity.type().equals("minecraft:old") ? "not a key!" : "minecraft:husk",
                    entity.nbt()
                );
            }
        };

        var read = roundTrip(buffer, GameDataProvider.NOOP, upgrade);

        assertEquals(
            List.of("minecraft:husk"),
            read.entities().stream().map(edit -> edit.data().type()).toList()
        );
    }

    @Test
    void aPaletteWithValuesNoCellUsesRoundTrips() {
        Block[] states = {Block.STONE, Block.DIRT, Block.GRANITE, Block.ANDESITE, Block.DIORITE};
        var builder = EditBuffer.builder();
        for (int y = 64; y < 80; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    builder.setBlockState(x, y, z, states[(x + y + z) % 5]);
        for (int y = 64; y < 80; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    if ((x + y + z) % 5 >= 3) builder.setBlockState(x, y, z, Block.STONE);
        var buffer = builder.build();
        var palette = Objects.requireNonNull(
            Objects.requireNonNull(buffer.section(0, 4, 0)).densePalette()
        );
        assertTrue(
            Objects.requireNonNull(PaletteHack.dictionary(palette)).size() > 3,
            "the palette still lists the overwritten states"
        );

        assertSameContent(buffer, roundTrip(buffer, GameDataProvider.NOOP, GameDataProvider.NOOP));
    }

    @Test
    void cellsPackedWiderThanNeededAreRead() {
        // Two values packed at 4 bits per cell, alternating stone and dirt
        byte[] bytes = NetworkBuffer.makeArray(out -> {
            out.write(NetworkBuffer.VAR_INT, 1);
            out.write(NetworkBuffer.VAR_INT, MinecraftServer.DATA_VERSION);
            out.write(NetworkBuffer.BOOLEAN, false);
            out.write(NetworkBuffer.VAR_INT, 1);
            out.write(NetworkBuffer.LONG, BlockUtil.packBlockPos(0, 4, 0));
            out.write(NetworkBuffer.BYTE, (byte) 3);
            out.write(NetworkBuffer.VAR_INT, 2);
            out.write(NetworkBuffer.VAR_INT, 1);
            out.write(NetworkBuffer.STRING, "minecraft:stone");
            out.write(NetworkBuffer.VAR_INT, 2);
            out.write(NetworkBuffer.STRING, "minecraft:dirt");
            out.write(NetworkBuffer.VAR_INT, 4);
            long[] packed = new long[256];
            Arrays.fill(packed, 0x1010101010101010L);
            out.write(NetworkBuffer.LONG_ARRAY, packed);
            out.write(NetworkBuffer.VAR_INT, 0);
            out.write(NetworkBuffer.BYTE, (byte) 0);
            out.write(NetworkBuffer.VAR_INT, 0);
        });

        EditBuffer read = read(bytes, GameDataProvider.NOOP);

        assertEquals(Block.STONE, read.getBlock(0, 64, 0));
        assertEquals(Block.DIRT, read.getBlock(1, 64, 0));
        assertEquals(Block.DIRT, read.getBlock(15, 79, 15));
    }

    @Test
    void aNewerFormatIsRejected() {
        byte[] bytes = NetworkBuffer.makeArray(buffer -> buffer.write(NetworkBuffer.VAR_INT, 99));

        assertThrows(IllegalStateException.class, () -> read(bytes, GameDataProvider.NOOP));
    }

    private static EditBuffer roundTrip(
        EditBuffer buffer,
        GameDataProvider writer,
        GameDataProvider reader
    ) {
        var type = EditBuffer.serializer(writer, MinecraftServer.getBiomeRegistry());
        return read(NetworkBuffer.makeArray(out -> out.write(type, buffer)), reader);
    }

    private static EditBuffer read(byte[] bytes, GameDataProvider reader) {
        var type = EditBuffer.serializer(reader, MinecraftServer.getBiomeRegistry());
        return NetworkBuffer.wrap(bytes, 0, bytes.length).read(type);
    }

    private static void assertSameContent(EditBuffer expected, EditBuffer actual) {
        assertEquals(expected.sectionCount(), actual.sectionCount());
        expected.forEachSection((sx, sy, sz, section) -> {
            var other = actual.section(sx, sy, sz);
            assertNotNull(other, "section " + sx + "," + sy + "," + sz);
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        assertEquals(section.getBlockState(x, y, z), other.getBlockState(x, y, z));
                        assertEquals(
                            section.getBlockEntity(x, y, z),
                            other.getBlockEntity(x, y, z)
                        );
                        if (x < 4 && y < 4 && z < 4)
                            assertEquals(section.getBiome(x, y, z), other.getBiome(x, y, z));
                    }
                }
            }
        });
    }

    private static int biome(String key) {
        var registry = MinecraftServer.getBiomeRegistry();
        return registry.getId(RegistryKey.<Biome>unsafeOf(Key.key(key)));
    }

    /// A provider a version past the writer's, so its upgrades run.
    private static class Newer implements GameDataProvider {
        @Override
        public int dataVersion() {
            return MinecraftServer.DATA_VERSION + 1;
        }
    }
}
