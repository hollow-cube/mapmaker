package net.hollowcube.terraform.instance;

import net.hollowcube.terraform.util.CoordinateUtil;
import net.hollowcube.util.schem.Rotation;
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
    private Rotation rotation = Rotation.NONE;


    public void addBlock(@NotNull Point point, @NotNull Block block) {
        blockSet.put(CoordinateUtil.floor(point), block);
    }

    public void setOffset(@NotNull Point point) {
        this.offset = point;
    }

    public void setRotation(@NotNull Rotation rotation) {
        this.rotation = rotation;
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
//        if (xSize * ySize * zSize > blockSet.size()) {
        paletteMap.put(Block.AIR, 0);
//        }

        ByteBuffer blockBytes = ByteBuffer.allocate(1024);
        for (int y = yMin; y <= yMax; y++) {
            for (int z = zMin; z <= zMax; z++) {
                for (int x = xMin; x <= xMax; x++) {
                    if (blockBytes.remaining() <= 3) {
                        byte[] oldBytes = blockBytes.array();
                        blockBytes = ByteBuffer.allocate(blockBytes.capacity() * 2);
                        blockBytes.put(oldBytes);
                    }

                    var pos = CoordinateUtil.floor(new Vec(x, y, z));
                    Block block = blockSet.get(pos);

                    if (block != null && !block.isAir()) {
                        int blockId;
                        if (!paletteMap.containsKey(block)) {
                            blockId = paletteMap.size();
                            paletteMap.put(block, paletteMap.size());
                        } else {
                            blockId = paletteMap.get(block);
                        }

                        Utils.writeVarInt(blockBytes, blockId);
                    } else {
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
                blockBytes.array(),
                rotation
        );
    }
}
