package net.hollowcube.mapmaker.hub.guiold.old;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class ItemUtils {

    public static final ItemStack BLANK_ITEM = ItemStack.of(Material.STICK)
            .withMeta(meta -> meta.customModelData(1000));

}
