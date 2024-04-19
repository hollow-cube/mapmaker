package net.hollowcube.mapmaker.map.item;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class ItemStackFixer {

    public static @NotNull ItemStack read1_20_4(@NotNull NetworkBuffer buffer) {
        boolean present = buffer.read(NetworkBuffer.BOOLEAN);
        if (!present) return ItemStack.AIR;

        int id = buffer.read(NetworkBuffer.VAR_INT);
        final Material material = Material.fromId(id);
        if (material == null) throw new RuntimeException("Unknown material id: " + id);

        final int amount = buffer.read(NetworkBuffer.BYTE);
        final BinaryTag nbt = buffer.read(NetworkBuffer.NBT);
        if (!(nbt instanceof CompoundBinaryTag compound)) {
            return ItemStack.of(material, amount);
        }
        return ItemStack.builder(material)
                .amount(amount)
                .set(ItemComponent.CUSTOM_DATA, new CustomData(compound))
                .build();
    }
}
