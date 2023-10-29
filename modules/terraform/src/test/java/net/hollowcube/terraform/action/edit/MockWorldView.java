package net.hollowcube.terraform.action.edit;

import net.hollowcube.terraform.selection.region.CuboidRegion;
import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;

public class MockWorldView implements WorldView {
    private final Block.Getter blockAccessor;

    public static @NotNull MockWorldView none() {
        return new MockWorldView(
                (x, y, z, condition) -> {
                    throw new AssertionError("No block should be accessed");
                }
        );
    }

    public static @NotNull MockWorldView of(@NotNull Block block) {
        return new MockWorldView((x, y, z, condition) -> block);
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }


    private MockWorldView(Block.Getter blockAccessor) {
        this.blockAccessor = blockAccessor;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        return blockAccessor.getBlock(x, y, z, condition);
    }

    public static class Builder {
        private final Map<Point, Block> blocks = new HashMap<>();
        private Block defaultBlock = null;

        public Builder defaultBlock(@NotNull Block block) {
            this.defaultBlock = block;
            return this;
        }

        public Builder fill(int x1, int y1, int z1, int x2, int y2, int z2, @NotNull Block block) {
            new CuboidRegion(new Vec(x1, y1, z1), new Vec(x2, y2, z2)).forEach(point -> blocks.put(point, block));
            return this;
        }

        public Builder set(int x, int y, int z, @NotNull Block block) {
            blocks.put(new Vec(x, y, z), block);
            return this;
        }

        public MockWorldView build() {
            return new MockWorldView((x, y, z, condition) -> {
                var block = blocks.get(new Vec(x, y, z));
                if (block == null) {
                    if (defaultBlock == null) {
                        throw new AssertionError(String.format("No block at %d, %d, %d", x, y, z));
                    } else {
                        return defaultBlock;
                    }
                } else {
                    return block;
                }
            });
        }
    }
}
