package net.hollowcube.terraform.compute;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public final class UtilityFunctions {

    public static @NotNull CountingFunction counting(@NotNull Region region, @NotNull Mask exclusionMask, boolean blocks, boolean states) {
        return new CountingFunction(region, exclusionMask, blocks, states);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class CountingFunction implements ComputeFunc {
        private Int2IntSortedMap countsByBlock = new Int2IntAVLTreeMap();
        private Int2IntSortedMap countsByState = new Int2IntAVLTreeMap();
        private long total;

        private final Region region;
        private final Mask exclusionMask;
        private final boolean blocks;
        private final boolean states;

        CountingFunction(@NotNull Region region, @NotNull Mask exclusionMask, boolean blocks, boolean states) {
            this.region = region;
            this.exclusionMask = exclusionMask;
            this.blocks = blocks;
            this.states = states;
        }

        @Override
        public @NotNull BlockBuffer exec(@NotNull Task task, @NotNull WorldView world) {
            for (var pos : region) {
                var block = world.getBlock(pos, Block.Getter.Condition.TYPE);
                if (exclusionMask.test(world, pos, block)) continue;

                if (blocks) countsByBlock.put(block.id(), countsByBlock.getOrDefault(block.id(), 0) + 1);
                if (states) countsByState.put(block.stateId(), countsByState.getOrDefault(block.stateId(), 0) + 1);
                total++;
            }

            return BlockBuffer.empty();
        }

        public @NotNull Int2IntSortedMap countsByBlock() {
            return countsByBlock;
        }

        public @NotNull Int2IntSortedMap countsByState() {
            return countsByState;
        }

        public long total() {
            return total;
        }
    }

    private UtilityFunctions() {
    }
}
