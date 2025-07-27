package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemUtils {

    private ItemUtils() {
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var key = material.key();
        String prefix = material.isBlock() ? "block" : "item";
        var translationKey = String.format("%s.%s.%s", prefix, key.namespace(), key.value());
        return Component.translatable(translationKey);
    }

    public static ItemStack asDisplay(@NotNull Material material) {
        return asDisplay(material, null);
    }

    public static ItemStack asDisplay(@NotNull Material material, @Nullable String overlay) {
        return OverlayItem.with(ItemStack.builder(Material.STICK), material, overlay).build();
    }
}
