package net.hollowcube.mapmaker.entity;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.time.Cooldown;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public interface PlayerCooldown {

    @NotNull Tag<Cooldown> cooldownTag();

    @NotNull Duration cooldownDuration();

    default boolean isOnCooldown(@NotNull Player player) {
        Cooldown cooldown = player.getTag(cooldownTag());
        return cooldown != null && !cooldown.isReady(System.currentTimeMillis());
    }

    /**
     * @param player The player who is trying to attempt an interaction with a cooldown
     * @param onUse  The code to run when the interaction is successful (we are off cooldown)
     */
    default void tryUseCooldown(@NotNull Player player, @NotNull Runnable onUse) {
        tryUseCooldown(player, onUse, () -> {
        });
    }

    /**
     * @param player    The player who is trying to attempt an interaction with a cooldown
     * @param onUse     The code to run when the interaction is successful (we are off cooldown)
     * @param onFailure The code to run when the interaction is on cooldown
     */
    default void tryUseCooldown(@NotNull Player player, @NotNull Runnable onUse, @NotNull Runnable onFailure) {
        // Check cooldown
        if (!player.hasTag(cooldownTag())) {
            // Player has never used a cooldown yet, add tag and run success
            onUse.run();
            player.setTag(cooldownTag(), new Cooldown(cooldownDuration()));
        } else {
            Cooldown cooldown = player.getTag(cooldownTag());
            if (cooldown.isReady(System.currentTimeMillis())) {
                onUse.run();
                // Seems a bit weird to allocate a new object every time we read
                player.tagHandler().updateTag(cooldownTag(), unused -> new Cooldown(cooldownDuration()));
            } else {
                onFailure.run();
            }
        }
    }
}
