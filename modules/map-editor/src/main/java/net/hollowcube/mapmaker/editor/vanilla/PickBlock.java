package net.hollowcube.mapmaker.editor.vanilla;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerPickBlockEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.ItemBlockState;

public final class PickBlock {

    public static void handlePickBlock(PlayerPickBlockEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) return; // Sanity
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return; // Sanity
        if (!world.canEdit(player)) return;

        // First try to get the block from the item registry
        var block = event.getBlock();
        ItemStack itemStack = world.itemRegistry().getItemStack(block, event.isIncludeData());

        // Otherwise create the item stack from the block
        if (itemStack == null) {
            var material = BlockUtil.getItem(block);
            if (material == null) return; // Sanity
            var builder = ItemStack.builder(material);
            if (event.isIncludeData() && !block.properties().isEmpty()) {
                builder.set(DataComponents.BLOCK_STATE, new ItemBlockState(block.properties()));
                builder.set(DataComponents.LORE, block.properties().entrySet().stream()
                        .<Component>map(entry -> Component.text()
                                .decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(entry.getKey(), NamedTextColor.GRAY))
                                .append(Component.text("=", NamedTextColor.DARK_GRAY))
                                .append(Component.text(entry.getValue(), NamedTextColor.WHITE))
                                .build())
                        .toList());
                builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
            itemStack = builder.build();
        }

        // Still no item, nothing to do
        if (itemStack == null) return;
        var inventory = player.getInventory();

        // If the item is already on the hotbar, swap to it
        for (int i = 0; i < 9; i++) {
            if (!inventory.getItemStack(i).isSimilar(itemStack))
                continue;
            player.setHeldItemSlot((byte) i);
            break;
        }

        int targetSlot = player.getHeldSlot();
        var targetItem = inventory.getItemStack(targetSlot);
        if (targetItem.isSimilar(itemStack)) return;
        if (!targetItem.isAir()) {
            // Try to find an empty slot
            for (int i = 0; i < 9; i++) {
                if (inventory.getItemStack(i).isAir()) {
                    targetSlot = i;
                    break;
                }
            }
            // If we didnt find an empty slot its fine we can keep the original and replace.
        }

        // If the item already exists in the inventory, swap to it
        int existingSlot = -1;
        for (int i = 9; i < inventory.getSize(); i++) {
            if (inventory.getItemStack(i).isSimilar(itemStack)) {
                existingSlot = i;
                break;
            }
        }

        if (existingSlot != -1) {
            var existingItem = inventory.getItemStack(existingSlot);
            inventory.setItemStack(existingSlot, itemStack);
            inventory.setItemStack(targetSlot, existingItem);
        } else {
            inventory.setItemStack(targetSlot, itemStack);
            if (targetSlot != player.getHeldSlot()) {
                player.setHeldItemSlot((byte) targetSlot);
            }
        }
    }

}
