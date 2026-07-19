package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class NbtUtilV2Test {

    @Test
    void readsEmptyItemIdAsAir() {
        var item = CompoundBinaryTag.builder()
            .putString("id", "")
            .build();

        assertSame(ItemStack.AIR, NbtUtilV2.readItemStack(item));
    }

    @Test
    void readsMalformedItemIdAsAir() {
        var item = CompoundBinaryTag.builder()
            .putString("id", "not an item")
            .build();

        assertSame(ItemStack.AIR, NbtUtilV2.readItemStack(item));
    }

    @Test
    void readsUnknownItemIdAsAir() {
        var item = CompoundBinaryTag.builder()
            .putString("id", "minecraft:not_an_item")
            .build();

        assertSame(ItemStack.AIR, NbtUtilV2.readItemStack(item));
    }

    @Test
    void readsMissingItemIdAsAir() {
        assertSame(ItemStack.AIR, NbtUtilV2.readItemStack(CompoundBinaryTag.empty()));
    }
}
