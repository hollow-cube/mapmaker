package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class NbtUtilTest {

    @Test
    void writesBlockWithIdAndProperties() {
        var tag = assertInstanceOf(CompoundBinaryTag.class, NbtUtil.writeBlock(Block.OAK_LOG.withProperty("axis", "x")));

        assertEquals("minecraft:oak_log", tag.getString("id"));
        assertEquals("x", tag.getCompound("properties").getString("axis"));
    }

    @Test
    void roundTripsBlock() {
        var block = Block.OAK_STAIRS.withProperty("facing", "west").withProperty("half", "top");
        assertEquals(block, NbtUtil.readBlock(NbtUtil.writeBlock(block)));
    }

    @Test
    void readsLegacyNameAndProperties() {
        var tag = CompoundBinaryTag.builder()
            .putString("Name", "minecraft:oak_log")
            .put("Properties", CompoundBinaryTag.builder().putString("axis", "z").build())
            .build();

        assertEquals(Block.OAK_LOG.withProperty("axis", "z"), NbtUtil.readBlock(tag));
    }
}
