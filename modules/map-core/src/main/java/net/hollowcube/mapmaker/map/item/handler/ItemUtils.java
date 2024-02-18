package net.hollowcube.mapmaker.map.item.handler;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var namespace = material.namespace();
        var translationKey = String.format("item.%s.%s", namespace.namespace(), namespace.path());
        return Component.translatable(translationKey);
    }
}
