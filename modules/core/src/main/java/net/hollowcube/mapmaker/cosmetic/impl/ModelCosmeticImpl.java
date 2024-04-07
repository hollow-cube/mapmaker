package net.hollowcube.mapmaker.cosmetic.impl;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.color.Color;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.hollowcube.mapmaker.cosmetic.Cosmetic.COSMETIC_TAG;

public class ModelCosmeticImpl extends CosmeticImpl {
    private final ItemStack model;

    public ModelCosmeticImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
        this.model = ItemStack.of(Material.LEATHER_HORSE_ARMOR)
                .withMeta(LeatherArmorMeta.class, meta -> {
                    meta.displayName(cosmetic.displayName());
                    meta.lore(cosmetic.lore());
                    var spritePath = "cosmetic/" + cosmetic.type().id() + "/" + cosmetic.id();
                    meta.customModelData(Objects.requireNonNull(BadSprite.SPRITE_MAP.get(spritePath), spritePath).cmd());
                    meta.color(new Color(255, 255, 255));
                    meta.hideFlag(ItemHideFlag.HIDE_DYE);
                    meta.setTag(COSMETIC_TAG, true);
                });
    }

    @Override
    public @NotNull ItemStack iconItem() {
        return model;
    }
}
