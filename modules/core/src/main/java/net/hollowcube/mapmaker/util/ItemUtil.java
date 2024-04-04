package net.hollowcube.mapmaker.util;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public final class ItemUtil {

    public static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(1))
            .build();

}
