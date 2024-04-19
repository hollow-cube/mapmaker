package net.hollowcube.mapmaker.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DepthStriderPotionHandler implements PotionHandler {
    @Override
    public void apply(@NotNull Player player, int level) {
        player.setBoots(ItemStack.builder(Material.IRON_BOOTS)
                .set(ItemComponent.ENCHANTMENTS, new EnchantmentList(Map.of(
                        Enchantment.DEPTH_STRIDER, level + 1,
                        Enchantment.BINDING_CURSE, 1
                ), false))
                .build());
    }

    @Override
    public void remove(@NotNull Player player) {
        player.setBoots(ItemStack.AIR);
    }
}
