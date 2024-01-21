package net.hollowcube.map.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class DepthStriderPotionHandler implements PotionHandler {
    @Override
    public void apply(@NotNull Player player, int level) {
        player.setBoots(ItemStack.builder(Material.IRON_INGOT)
                .meta(meta -> {
                    meta.enchantment(Enchantment.DEPTH_STRIDER, (short) (level + 1));
//                    meta.hideFlag(ItemHideFlag.HIDE_ENCHANTS);
                })
                .build());
    }

    @Override
    public void remove(@NotNull Player player) {
        player.setBoots(ItemStack.AIR);
    }
}
