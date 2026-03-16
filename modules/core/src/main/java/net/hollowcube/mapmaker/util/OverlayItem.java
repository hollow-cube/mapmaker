package net.hollowcube.mapmaker.util;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class OverlayItem {

    private static final BadSprite VANILLA_ITEM = BadSprite.require("vanilla_item");
    public static final String OVERLAY_ITEM_MODEL = VANILLA_ITEM.model();

    public static ItemStack.Builder with(ItemStack.Builder builder, Material material, @Nullable String overlay) {
        String model = Objects.requireNonNullElse(material.prototype().get(DataComponents.ITEM_MODEL), "paper");
        if (model.startsWith("minecraft:")) {
            model = model.substring("minecraft:".length());
        }

        builder.set(DataComponents.ITEM_MODEL, OverlayItem.OVERLAY_ITEM_MODEL);
        builder.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
            List.of(),
            List.of(),
            overlay == null ? List.of(model) : List.of(model, overlay),
            List.of()
        ));

        return builder;
    }

    public static ItemStack with(ItemStack stack, Material material, @Nullable String overlay) {
        String model = Objects.requireNonNullElse(material.prototype().get(DataComponents.ITEM_MODEL), "paper");
        if (model.startsWith("minecraft:")) {
            model = model.substring("minecraft:".length());
        }

        return stack.with(DataComponents.ITEM_MODEL, OverlayItem.OVERLAY_ITEM_MODEL)
            .with(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(),
                List.of(),
                overlay == null ? List.of(model) : List.of(model, overlay),
                List.of()
            ));
    }

    private static List<String> getModelData(ItemStack stack) {
        var customModelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (customModelData == null || customModelData.strings().isEmpty()) return List.of();
        if (!Objects.equals(stack.get(DataComponents.ITEM_MODEL), OverlayItem.OVERLAY_ITEM_MODEL)) return List.of();
        return customModelData.strings();
    }

    public static @Nullable String getOverlay(ItemStack stack) {
        var data = getModelData(stack);
        return data.isEmpty() || data.size() < 2 ? null : data.get(1);
    }

    public static @Nullable String getBaseModel(ItemStack stack) {
        var data = getModelData(stack);
        return data.isEmpty() ? null : data.getFirst();
    }
}
