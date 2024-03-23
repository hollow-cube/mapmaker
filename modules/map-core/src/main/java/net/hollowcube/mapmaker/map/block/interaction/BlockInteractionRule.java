package net.hollowcube.mapmaker.map.block.interaction;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

/**
 * Represents a rule that handles interactions with a block.
 *
 * <p>This can be registered _either_ to a specific block type or to a specific item type.
 * Assignments to blocks take precedence over item interactions. For example, with a water
 * bucket (to place water) attached to the water bucket item, and a trapdoor opening
 * interaction. The trapdoor will be opened if holding a water bucket.</p>
 */
public interface BlockInteractionRule {

    enum SneakState {
        NOT_SNEAKING,
        SNEAKING,
        BOTH,
        NOT_SNEAKING_OR_EMPTY_HAND;

        public boolean test(boolean isSneaking, boolean hasItemInHand) {
            return this == BOTH || (this == SNEAKING && isSneaking) || (this == NOT_SNEAKING && !isSneaking) || (this == NOT_SNEAKING_OR_EMPTY_HAND && (!isSneaking || !hasItemInHand));
        }
    }

    /**
     * Tries to handle the particular action, or returns false if the interaction
     * is not applicable, allowing the next one to be handled in the chain.
     *
     * @param interaction The interaction that occurred.
     * @return True to stop the chain, false to continue.
     */
    boolean handleInteraction(@NotNull Interaction interaction);

    default @NotNull SneakState sneakState() {
        return SneakState.NOT_SNEAKING;
    }

    /**
     * An additional interaction type for right-clicking with an item on air. Must be used on a class
     * which is already a registered BlockInteractionRule.
     *
     * <p>Air Interaction objects always contain null values for blockPosition and blockFace.</p>
     */
    interface AirInteractionRule {
        boolean handleAirInteraction(@NotNull Interaction interaction);
    }

    record Interaction(
            @NotNull Player player, @NotNull Instance instance, @UnknownNullability Point blockPosition,
            @UnknownNullability BlockFace blockFace, @Nullable Point cursorPosition,
            @NotNull ItemStack item, Player.@NotNull Hand hand
    ) implements Block.Getter, Block.Setter {

        public @NotNull WorldBorder worldBorder() {
            return instance.getWorldBorder();
        }

        public boolean worldContains(@NotNull Point point) {
            return instance.getWorldBorder().isInside(point)
                    && point.blockY() >= instance.getDimensionType().getMinY()
                    && point.blockY() <= instance.getDimensionType().getMaxY();
        }

        @Override
        public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Block.Getter.Condition condition) {
            return instance.getBlock(x, y, z, condition);
        }

        @Override
        public void setBlock(int x, int y, int z, @NotNull Block block) {
            var blockPosition = new Vec(x, y, z);
            // Never set a block outside the border.
            if (!instance.getWorldBorder().isInside(blockPosition)) return;
            instance.setBlock(blockPosition, block);
        }

        public void placeBlock(@NotNull Point blockPosition, @NotNull Block block) {
            // Never set a block outside the border.
            if (!instance.getWorldBorder().isInside(blockPosition)) return;

            var cursorPosition = Objects.requireNonNullElse(cursorPosition(), Vec.ZERO);
            instance.placeBlock(new BlockHandler.PlayerPlacement(
                    block, instance, blockPosition,
                    player, hand, blockFace,
                    (float) cursorPosition.x(),
                    (float) cursorPosition.y(),
                    (float) cursorPosition.z()));
        }

        public void playSound(@NotNull Sound sound, @NotNull Point blockPosition) {
            instance.playSound(sound, blockPosition);
        }

        public void playBlockSound(@NotNull SoundEvent sound, float volume, float pitch) {
            instance.playSound(Sound.sound(sound, Sound.Source.BLOCK, volume, pitch), blockPosition);
        }
    }
}
