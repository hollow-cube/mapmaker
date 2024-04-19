package net.hollowcube.terraform.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.block.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public final class PlayerUtil {

    /**
     * The builtin version of this function takes an integer... for some reason.
     */
    public static @Nullable Point getTargetBlock(@NotNull Player player, double maxDistance) {
        try {
            var instance = player.getInstance();
            if (instance == null) return null;
            var pos = player.getPosition();

            Iterator<Point> it = new BlockIterator(pos.asVec(), pos.direction(),
                    player.getEyeHeight(), maxDistance, false);
            while (it.hasNext()) {
                final Point position = it.next();
                if (!instance.getBlock(position, Block.Getter.Condition.TYPE).isAir()) return position;
            }
        } catch (NullPointerException e) {
            if (!e.getMessage().contains("Unloaded chunk"))
                throw new RuntimeException(e);
        }
        return null;
    }

    public static void smartAddItemStack(@NotNull Player player, @NotNull ItemStack itemStack) {
        // If their current item is empty, just set it
        var current = player.getItemInMainHand();
        if (current.isAir()) {
            player.setItemInMainHand(itemStack);
            return;
        }

        // Otherwise, try another hotbar slot
        for (int slot = 0; slot < 9; slot++) {
            var slotItem = player.getInventory().getItemStack(slot);
            if (slotItem.isAir()) {
                player.getInventory().setItemStack(slot, itemStack);
                return;
            }
        }

        // Otherwise, try to add normally to inventory
        var added = player.getInventory().addItemStack(itemStack, TransactionOption.ALL_OR_NOTHING);
        if (added) return;

        // Finally, just replace their main hand item
        player.setItemInMainHand(itemStack);
    }

    private PlayerUtil() {
    }
}
