package net.hollowcube.mapmaker.cosmetic.impl;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.color.Color;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.cosmetic.Cosmetic.COSMETIC_TAG;

public class ModelCosmeticImpl extends CosmeticImpl {
    private final ItemStack model;

    public ModelCosmeticImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
        this.model = ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
                .set(ItemComponent.CUSTOM_NAME, cosmetic.displayName())
                .set(ItemComponent.LORE, cosmetic.lore())
                .set(ItemComponent.CUSTOM_MODEL_DATA, BadSprite.require("cosmetic/" + cosmetic.type().id() + "/" + cosmetic.id()).cmd())
                .set(ItemComponent.DYED_COLOR, new DyedItemColor(new Color(255, 255, 255), false))
                .remove(ItemComponent.ATTRIBUTE_MODIFIERS)
                .build()
                // stupid, builder settag should return the builder...
                .withTag(COSMETIC_TAG, true);
    }

    @Override
    public @NotNull ItemStack iconItem() {
        return model;
    }
}
