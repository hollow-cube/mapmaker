package net.hollowcube.mapmaker.map.block.interaction;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class BonemealInteractionRule implements BlockInteractionRule {
    private static final Int2IntMap AGEABLE_BLOCKS = new Int2IntArrayMap();

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        // Special cases for stacking cactus and sugar cane
        if (block.id() == Block.CACTUS.id() || block.id() == Block.SUGAR_CANE.id()) {
            while (interaction.worldContains(blockPosition)) {
                blockPosition = blockPosition.add(0, 1, 0);
                var nextBlock = interaction.getBlock(blockPosition);
                if (nextBlock.isAir()) {
                    interaction.setBlock(blockPosition, block);
                } else if (nextBlock.id() != block.id()) {
                    break;
                }
            }
            return true;
        }

        // Generic ageable
        int maxAge = AGEABLE_BLOCKS.getOrDefault(block.id(), -1);
        if (maxAge == -1) return false;
        int age = Integer.parseInt(block.getProperty("age"));
        if (age == maxAge && block.id() == Block.TORCHFLOWER_CROP.id()) {
            block = Block.TORCHFLOWER;
        } else {
            age++;


            if (block.id() == Block.PITCHER_CROP.id()) {
                if (age > maxAge) return false;

                block = block.withProperty("age", String.valueOf(age));

                var half = block.getProperty("half");
                var otherPosition = blockPosition.add(0, "lower".equals(half) ? 1 : -1, 0);
                var otherBlock = interaction.getBlock(otherPosition);

                // Place the upper block at age 3
                if (age == 3 && "lower".equals(half) && otherBlock.isAir()) {
                    otherBlock = Block.PITCHER_CROP.withProperty("half", "upper");
                }

                // If there is a pitcher at the opposite side and it is the opposite half, update it too.
                if (otherBlock.id() == Block.PITCHER_CROP.id() && !half.equals(otherBlock.getProperty("half"))) {
                    interaction.setBlock(otherPosition, otherBlock.withProperty("age", String.valueOf(age)));
                }
            } else {
                age %= maxAge;
                block = block.withProperty("age", String.valueOf(age));
            }
        }

        interaction.setBlock(blockPosition, block);
        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    static {
        for (var block : Block.values()) {
            if (block.getProperty("age") == null) continue;

            int maxAge = 0;
            for (var state : block.possibleStates())
                maxAge = Math.max(maxAge, Integer.parseInt(state.getProperty("age")));
            AGEABLE_BLOCKS.put(block.id(), maxAge);
        }
    }
}
