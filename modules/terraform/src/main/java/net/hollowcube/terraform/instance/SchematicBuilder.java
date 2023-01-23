package net.hollowcube.terraform.instance;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SchematicBuilder {

    private final Map<Point, Block> blockSet = new HashMap<>();

    private Point offset = Vec.ZERO;


    public void addBlock(@NotNull Point point, @NotNull Block block) {
        blockSet.put(point, block);
    }

    public void setOffset(@NotNull Point point) {
        this.offset = point;
    }

    public Schematic toSchematic() {
        // Find lowest and highest x, y, z
        int xMin = Integer.MAX_VALUE;
        int yMin = Integer.MAX_VALUE;
        int zMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMax = Integer.MIN_VALUE;
        int zMax = Integer.MIN_VALUE;

        for (Point point : blockSet.keySet()) {
            if (point.blockX() < xMin) {
                xMin = point.blockX();
            }
            if (point.blockX() > xMax) {
                xMax = point.blockX();
            }
            if (point.blockY() < yMin) {
                yMin = point.blockY();
            }
            if (point.blockY() > yMax) {
                yMax = point.blockY();
            }
            if (point.blockZ() < zMin) {
                zMin = point.blockZ();
            }
            if (point.blockZ() > zMax) {
                zMax = point.blockZ();
            }
        }
        int xSize = xMax - xMin + 1;
        int ySize = yMax - yMin + 1;
        int zSize = zMax - zMin + 1;
        Point size = new Vec(xSize, ySize, zSize);
        offset = offset.add(new Vec(xMin, yMin, zMin));

        // Map of Block -> Palette ID
        HashMap<Block, Integer> paletteMap = new HashMap<>();

        // Determine if we have air in our palette
        // If the number of blocks in our blockset is equal to our size, we know we shouldn't fill in air as default since we have taken up every space
        if (xSize * ySize * zSize > blockSet.size()) {
            paletteMap.put(Block.AIR, 0);
        }

        // This is horribly memory and space inefficient, but I cannot think of a better way of doing this right now
        ByteBuffer blockBytes = ByteBuffer.allocate(1024);
        Set<Point> pointSet = blockSet.keySet();
        for (int y = yMin; y <= yMax; y++) {
            for (int z = zMin; z <= zMax; z++) {
                for (int x = xMin; x <= xMax; x++) {
                    // Should be okay, since this is a short
                    // Also matt said so
                    if (blockBytes.remaining() <= 3) {
                        byte[] oldBytes = blockBytes.array();
                        blockBytes = ByteBuffer.allocate(blockBytes.capacity() * 2);
                        blockBytes.put(oldBytes);
                    }
                    boolean foundPoint = false;
                    for (Point point : pointSet) {
                        if (point.blockX() == x && point.blockY() == y && point.blockZ() == z) {
                            Block block = blockSet.get(point);
                            int blockId;
                            if (!paletteMap.containsKey(block)) {
                                blockId = paletteMap.size();
                                paletteMap.put(block, paletteMap.size());
                            } else {
                                blockId = paletteMap.get(block);
                            }
                            foundPoint = true;
                            Utils.writeVarInt(blockBytes, blockId);
                            break;
                        }
                    }
                    if (!foundPoint) {
                        Utils.writeVarInt(blockBytes, 0);
                    }
                }
            }
        }

        Block[] palette = new Block[paletteMap.size()];
        for (var entry : paletteMap.entrySet()) {
            palette[entry.getValue()] = entry.getKey();
        }

        return new Schematic(
                size,
                offset,
                palette,
                blockBytes.array()
        );
    }
}
