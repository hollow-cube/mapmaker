package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public final class ItemUtils {

    private ItemUtils() {
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var key = material.key();
        String prefix = material.isBlock() ? "block" : "item";
        var translationKey = String.format("%s.%s.%s", prefix, key.namespace(), key.value());
        return Component.translatable(translationKey);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack asDisplay(@NotNull Material material) {
        var key = material.key();
        String model = material.prototype().get(DataComponents.ITEM_MODEL);
        return ItemStack.builder(Material.STICK)
                .set(DataComponents.ITEM_MODEL, model)
                .build();
    }
}
