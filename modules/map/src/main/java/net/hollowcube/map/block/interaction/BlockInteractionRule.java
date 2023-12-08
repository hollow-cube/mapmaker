package net.hollowcube.map.block.interaction;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

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
        BOTH;

        public boolean test(boolean isSneaking) {
            return this == BOTH || (this == SNEAKING && isSneaking) || (this == NOT_SNEAKING && !isSneaking);
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

    final class Interaction implements Block.Getter, Block.Setter {
        private final Player player;
        private final Instance instance;
        private final Point blockPosition;
        private final BlockFace blockFace;
        private final ItemStack item;
        private final Player.Hand hand;

        public Interaction(
                @NotNull Player player,
                @NotNull Instance instance,
                @NotNull Point blockPosition,
                @NotNull BlockFace blockFace,
                @NotNull ItemStack item,
                @NotNull Player.Hand hand
        ) {
            this.player = player;
            this.instance = instance;
            this.blockPosition = blockPosition;
            this.blockFace = blockFace;
            this.item = item;
            this.hand = hand;
        }

        public @NotNull Player player() {
            return player;
        }

        public @NotNull Point blockPosition() {
            return blockPosition;
        }

        public @NotNull BlockFace blockFace() {
            return blockFace;
        }

        public @NotNull ItemStack item() {
            return item;
        }

        public Player.@NotNull Hand hand() {
            return hand;
        }

        public @NotNull WorldBorder worldBorder() {
            return instance.getWorldBorder();
        }

        @Override
        public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Block.Getter.Condition condition) {
            return instance.getBlock(x, y, z, condition);
        }

        @Override
        public void setBlock(int x, int y, int z, @NotNull Block block) {
            // Never set a block outside the border.
            if (!instance.getWorldBorder().isInside(new Vec(x, y, z))) return;

            instance.setBlock(x, y, z, block);
        }

        public void playSound(@NotNull Sound sound, @NotNull Point blockPosition) {
            instance.playSound(sound, blockPosition);
        }
    }
}
