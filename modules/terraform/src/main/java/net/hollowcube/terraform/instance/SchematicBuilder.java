package net.hollowcube.terraform.instance;

import net.hollowcube.terraform.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SchematicBuilder {

    private final Map<Point, Block> blockSet = new ConcurrentHashMap<>();

    private Point offset = Vec.ZERO;


    public void addBlock(@NotNull Point point, @NotNull Block block) {
        var pos = CoordinateUtil.floor(point);
        blockSet.put(pos, block);
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
        offset = offset.add(xMin, yMin, zMin);

        // Map of Block -> Palette ID
        HashMap<Block, Integer> paletteMap = new HashMap<>();

        // Determine if we have air in our palette
        // If the number of blocks in our blockset is equal to our size, we know we shouldn't fill in air as default since we have taken up every space
        if(xSize * ySize * zSize > blockSet.size()) {
            paletteMap.put(Block.AIR, 0);
        }

        ByteBuffer blockBytes = ByteBuffer.allocate(1024);
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    if (blockBytes.remaining() <= 3) {
                        byte[] oldBytes = blockBytes.array();
                        blockBytes = ByteBuffer.allocate(blockBytes.capacity() * 2);
                        blockBytes.put(oldBytes);
                    }

                    var block = blockSet.get(new Vec(x, y, z));
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
                        blockId = paletteMap.get(block);
                    }
                    Utils.writeVarInt(blockBytes, blockId);
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
