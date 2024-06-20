package net.hollowcube.mapmaker.map.item;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static @NotNull CompoundBinaryTag getEntityTag(@NotNull ItemStack itemStack) {
        return itemStack.get(ItemComponent.BLOCK_ENTITY_DATA, CustomData.EMPTY).nbt();
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var namespace = material.namespace();
        String prefix = material.isBlock() ? "block" : "item";
        var translationKey = String.format("%s.%s.%s", prefix, namespace.namespace(), namespace.path());
        return Component.translatable(translationKey);
    }
}
