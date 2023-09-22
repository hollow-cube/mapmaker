package net.hollowcube.terraform.schem;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.batch.BatchOption;
import net.minestom.server.instance.batch.RelativeBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * Represents a schematic file which can be manipulated in the world.
 */
@SuppressWarnings("UnstableApiUsage")
public record Schematic(
        Point size,
        Point offset,
        Block[] palette,
        byte[] blocks
) {
    private static final System.Logger logger = System.getLogger(Schematic.class.getName());

    static final Schematic EMPTY = new Schematic(Vec.ZERO, Vec.ZERO, new Block[0], new byte[0]);

    public Schematic {
        palette = Arrays.copyOf(palette, palette.length);
        blocks = Arrays.copyOf(blocks, blocks.length);
    }

    public @NotNull Point size(@NotNull Rotation rotation) {
        return CoordinateUtil.abs(CoordinateUtil.rotatePos(size, rotation));
    }

    public @NotNull Point offset(@NotNull Rotation rotation) {
        return CoordinateUtil.rotatePos(offset, rotation);
    }

    /**
     * Apply the schematic directly given a rotation. The applicator function will be called for each block in the schematic.
     * <p>
     * Note: The {@link Point} passed to `applicator` is relative to the {@link #offset()}.
     *
     * @param rotation   The rotation to apply before placement.
     * @param applicator The function to call for each block in the schematic.
     */
    public void apply(@NotNull Rotation rotation, @NotNull BiConsumer<Point, Block> applicator) {
        var blocks = ByteBuffer.wrap(this.blocks);
        for (int y = 0; y < size().y(); y++) {
            for (int z = 0; z < size().z(); z++) {
                for (int x = 0; x < size().x(); x++) {
                    var block = palette[Utils.readVarInt(blocks)];
                    if (block == null) {
                        logger.log(System.Logger.Level.WARNING, "Missing palette entry at {0}, {1}, {2}", x, y, z);
                        block = Block.AIR;
                    }

                    applicator.accept(
                            CoordinateUtil.rotatePos(offset.add(x, y, z), rotation),
                            CoordinateUtil.rotateBlock(block, rotation));
                }
            }
        }
    }

    /**
     * Convert the schematic into a {@link RelativeBlockBatch} which can be applied to an instance.
     * The schematic can be rotated around its {@link #offset()} before placement.
     *
     * @param rotation      The rotation to apply to the schematic.
     * @param blockModifier If present, called on each individual block before it is placed.
     * @return A {@link RelativeBlockBatch} which represents the schematic file at its offset.
     */
    public @NotNull RelativeBlockBatch build(@NotNull Rotation rotation, @Nullable UnaryOperator<Block> blockModifier) {
        RelativeBlockBatch batch = new RelativeBlockBatch(new BatchOption().setCalculateInverse(true));
        apply(rotation, (pos, block) -> batch.setBlock(pos, blockModifier == null ? block : blockModifier.apply(block)));
        return batch;
    }

    public @NotNull RelativeBlockBatch build(@NotNull Rotation rotation, boolean skipAir) {
        RelativeBlockBatch batch = new RelativeBlockBatch(new BatchOption().setCalculateInverse(true));
        apply(rotation, (pos, block) -> {
            if (block.isAir() && skipAir) return;
            batch.setBlock(pos, block);
        });
        return batch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schematic schematic = (Schematic) o;
        return size.equals(schematic.size) &&
                offset.equals(schematic.offset) &&
                Arrays.equals(palette, schematic.palette) &&
                Arrays.equals(blocks, schematic.blocks);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(size, offset);
        result = 31 * result + Arrays.hashCode(palette);
        result = 31 * result + Arrays.hashCode(blocks);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "Schematic[size=%s, offset=%s, palette=%s, blocks=%s]",
                size, offset, Arrays.toString(palette), Arrays.toString(blocks)
        );
    }
}
