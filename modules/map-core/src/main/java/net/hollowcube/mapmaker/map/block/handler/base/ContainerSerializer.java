package net.hollowcube.mapmaker.map.block.handler.base;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ContainerSerializer implements TagSerializer<List<ItemStack>> {

    private final Tag<List<BinaryTag>> RAW = Tag.NBT("Items").list();
    private final int maxSize;

    public ContainerSerializer(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public @Nullable List<ItemStack> read(@NotNull TagReadable tag) {
        var raw = tag.getTag(RAW);
        if (raw == null) return null;

        var items = new ArrayList<ItemStack>(this.maxSize);
        for (int i = 0; i < this.maxSize; i++) items.add(ItemStack.AIR);

        for (var nbt : raw) {
            var compound = (CompoundBinaryTag) nbt;
            var slot = compound.getByte("Slot");
            if (slot < 0 || slot >= this.maxSize) continue;
            items.set(slot, ItemStack.fromItemNBT(compound));
        }
        return items;
    }

    @Override
    public void write(@NotNull TagWritable tagWritable, @NotNull List<ItemStack> itemStacks) {
        var raw = new ArrayList<BinaryTag>();
        for (int i = 0; i < Math.min(itemStacks.size(), this.maxSize); i++) {
            var stack = itemStacks.get(i);
            if (stack.isAir()) continue;
            raw.add(itemStacks.get(i).toItemNBT().putByte("Slot", (byte) i));
        }
        tagWritable.setTag(RAW, raw);
    }
}