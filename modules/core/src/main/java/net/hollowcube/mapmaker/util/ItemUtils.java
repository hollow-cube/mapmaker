package net.hollowcube.mapmaker.util;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class ItemUtils {

    private static final BadSprite VANILLA_ITEM = BadSprite.require("vanilla_item");


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

    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack asDisplay(@NotNull Material material, @Nullable String overlay) {
        String model = Objects.requireNonNullElse(material.prototype().get(DataComponents.ITEM_MODEL), "paper");
        if (model.startsWith("minecraft:")) {
            model = model.substring("minecraft:".length());
        }


        return ItemStack.builder(Material.STICK)
                .set(DataComponents.ITEM_MODEL, VANILLA_ITEM.model())
                .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                        List.of(),
                        List.of(),
                        overlay == null ? List.of(model) : List.of(model, overlay),
                        List.of()
                ))
                .build();
    }
}
