package net.hollowcube.terraform.schem;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class SchematicBuilder {

    // Point -> Block, a missing value is air
    private final Map<Point, Block> blockSet = new ConcurrentHashMap<>();

    private Point offset = Vec.ZERO;

    public void addBlock(double x, double y, double z, @NotNull Block block) {
        addBlock(new Vec(x, y, z), block);
    }

    public void addBlock(@NotNull Point point, @NotNull Block block) {
        var pos = CoordinateUtil.floor(point);
        if (block.isAir()) {
            // If it's air, attempt to remove any existing value at that point.
            blockSet.remove(pos);
        } else {
            blockSet.put(pos, block);
        }
    }

    public void setOffset(double x, double y, double z) {
        setOffset(new Vec(x, y, z));
    }

    public void setOffset(@NotNull Point point) {
        this.offset = point;
    }

    public @NotNull Schematic build() {
        Point min = new Vec(Double.MAX_VALUE), max = new Vec(Double.MIN_VALUE);
        for (Point point : blockSet.keySet()) {
            min = CoordinateUtil.min(min, point);
            max = CoordinateUtil.max(max, point);
        }

        var size = max.sub(min).add(1);
        offset = offset.add(min);
        var blockCount = size.blockX() * size.blockY() * size.blockZ();

        // Map of Block -> Palette ID
        Object2IntMap<Block> paletteMap = new Object2IntArrayMap<>();

        // If the number of blocks set in the builder is not equal to the size of the schematic,
        // then there must be at least one air block. We just add this immediately.
        // There can never be an air block in the map because #addBlock does a remove if the block is air.
        // We always keep air as palette block zero, since it is likely the vast
        // majority of blocks, so we want to ensure that it is one byte.
        if (blockSet.size() != blockCount) {
            paletteMap.put(Block.AIR, 0);
        }

        // Write each block to the output buffer
        // Initial buffer size assumes that we have a palette less than 127
        // so each block is one byte. If the palette is larger, we will resize
        var blockBytes = ByteBuffer.allocate(blockCount + 4);
        for (double i = 0; i < blockCount; i++) {

            // Resize array if it is too small
            if (blockBytes.remaining() <= 3) {
                byte[] oldBytes = blockBytes.array();
                blockBytes = ByteBuffer.allocate(blockBytes.capacity() * 2);
                blockBytes.put(oldBytes);
            }

            // Write the block
            var blockPos = min.add(new Vec(
                    (int) i % size.blockX(),
                    (int) (i / (size.blockX() * size.blockY())),
                    (int) (i / size.blockX()) % size.blockY()
            ));
            var block = blockSet.get(blockPos);

            if (block == null) {
                // Block not set, write an air value
                Utils.writeVarInt(blockBytes, 0);
                continue;
            }

            int blockId;
            if (!paletteMap.containsKey(block)) {
                blockId = paletteMap.size();
                paletteMap.put(block, paletteMap.size());
            } else {
                blockId = paletteMap.getInt(block);
            }

            Utils.writeVarInt(blockBytes, blockId);
        }

        var palette = new Block[paletteMap.size()];
        for (var entry : paletteMap.object2IntEntrySet()) {
            palette[entry.getIntValue()] = entry.getKey();
        }

        var out = new byte[blockBytes.position()];
        blockBytes.flip().get(out);

        return new Schematic(size, offset, palette, out);
    }
}
