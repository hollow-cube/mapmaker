package net.hollowcube.mapmaker.map.util;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface PlayerCooldownExtension {

    static boolean tryUseItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (player instanceof PlayerCooldownExtension cooldownExtension)
            return cooldownExtension.tryUseItem(itemStack);
        return true; // Allow it anyway
    }

    /// Attempts to use the item accounting for its cooldown group.
    /// Used items will have their cooldown started.
    ///
    /// @return true if the item was used, false if it was on cooldown.
    boolean tryUseItem(@NotNull ItemStack itemStack);

}
