package net.hollowcube.mapmaker.cosmetic.impl;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.color.Color;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.cosmetic.Cosmetic.COSMETIC_TAG;

public class ModelCosmeticImpl extends CosmeticImpl {
    private final ItemStack model;

    public ModelCosmeticImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
        this.model = ItemStack.builder(Material.STICK)
                .set(DataComponents.CUSTOM_NAME, cosmetic.displayName())
                .set(DataComponents.LORE, cosmetic.lore())
                .set(DataComponents.ITEM_MODEL, BadSprite.require("cosmetic/" + cosmetic.type().id() + "/" + cosmetic.id()).model())
                .set(DataComponents.DYED_COLOR, new Color(255, 255, 255))
                .hideExtraTooltip()
                .build()
                // stupid, builder settag should return the builder...
                .withTag(COSMETIC_TAG, true);
    }

    @Override
    public @NotNull ItemStack iconItem() {
        return model;
    }
}
