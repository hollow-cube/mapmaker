package net.hollowcube.mapmaker.cosmetic.impl;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.color.Color;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static net.hollowcube.mapmaker.cosmetic.Cosmetic.COSMETIC_TAG;

public class ModelCosmeticImpl extends CosmeticImpl {

    private static final Set<DataComponent<?>> HIDDEN_TOOLTIPS = Set.of(
            DataComponents.BANNER_PATTERNS,
            DataComponents.BEES,
            DataComponents.BLOCK_ENTITY_DATA,
            DataComponents.BLOCK_STATE,
            DataComponents.BUNDLE_CONTENTS,
            DataComponents.CHARGED_PROJECTILES,
            DataComponents.CONTAINER,
            DataComponents.CONTAINER_LOOT,
            DataComponents.FIREWORK_EXPLOSION,
            DataComponents.FIREWORKS,
            DataComponents.INSTRUMENT,
            DataComponents.MAP_ID,
            DataComponents.PAINTING_VARIANT,
            DataComponents.POT_DECORATIONS,
            DataComponents.POTION_CONTENTS,
            DataComponents.TROPICAL_FISH_PATTERN,
            DataComponents.WRITTEN_BOOK_CONTENT,
            DataComponents.DYED_COLOR
    );

    private final ItemStack model;

    public ModelCosmeticImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
        this.model = ItemStack.builder(Material.STICK)
                .set(DataComponents.CUSTOM_NAME, cosmetic.displayName())
                .set(DataComponents.LORE, cosmetic.lore())
                .set(DataComponents.ITEM_MODEL, BadSprite.require("cosmetic/" + cosmetic.type().id() + "/" + cosmetic.id()).model())
                .set(DataComponents.DYED_COLOR, new Color(255, 255, 255))
                .set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, HIDDEN_TOOLTIPS))
                .set(COSMETIC_TAG, true)
                .build();
    }

    @Override
    public @NotNull ItemStack iconItem() {
        return model;
    }
}
