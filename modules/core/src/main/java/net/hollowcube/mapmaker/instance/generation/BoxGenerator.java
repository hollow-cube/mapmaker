package net.hollowcube.mapmaker.instance.generation;

import net.hollowcube.mapmaker.map.BoxType;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoxGenerator implements Generator {
    private static final int BOX_SIZE = 17;
    private static final int BOX_HEIGHT = 21;
    private static final int BOTTOM_HEIGHT = 36;
    private static final int TOP_HEIGHT = BOTTOM_HEIGHT + BOX_HEIGHT;
    private final Map<ChunkCoordinatePair, List<Point>> blocksToSet = new HashMap<>();

    public BoxGenerator(BoxType type) {
        int min_x = -BOX_SIZE / 2;
        int max_x = BOX_SIZE / 2;
        int min_z = -1;
        int max_z = BOX_SIZE - 2;
        for (int x = min_x; x <= max_x; x++) {
            for (int z = min_z; z <= max_z; z++) {
                for (int y = BOTTOM_HEIGHT; y < TOP_HEIGHT; y++) {
                    BoxGenerator.ChunkCoordinatePair pair = new BoxGenerator.ChunkCoordinatePair(
                            (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X),
                            (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z)
                    );
                    blocksToSet.putIfAbsent(pair, new ArrayList<>());

                    // Always place the corners
                    if ((x == min_x || x == max_x) && (z == min_z || z == max_z))
                        blocksToSet.get(pair).add(new Pos(x, y, z));

                    // If at bottom or top of box, place square
                    if ((y == BOTTOM_HEIGHT || y == TOP_HEIGHT - 1) &&
                            (x == min_x || x == max_x || z == min_z || z == max_z))
                        blocksToSet.get(pair).add(new Pos(x, y, z));

                    // Place start block
                    if (x == 0 && y == BOTTOM_HEIGHT + 3 && z == 0)
                        blocksToSet.get(pair).add(new Pos(x, y, z));

                    // Place end block
                    if (type.equals(BoxType.STRAIGHT)) {
                        if (x == 0 && y == BOTTOM_HEIGHT + 3 && z == max_z - 1)
                            blocksToSet.get(pair).add(new Pos(x, y, z));
                    }
                    else if (x == max_x && y == BOTTOM_HEIGHT + 3 && z == (min_z + max_z) / 2)
                            blocksToSet.get(pair).add(new Pos(x, y, z));
                }
            }
        }
    }

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Place all points we precalculated earlier
        var pair = new BoxGenerator.ChunkCoordinatePair(unit.absoluteStart().chunkX(), unit.absoluteStart().chunkZ());
        if (blocksToSet.containsKey(pair)) {
            for (Point point : blocksToSet.get(pair)) {
                unit.modifier().setBlock(point, Block.STONE);
            }
        }
    }

    private record ChunkCoordinatePair(int x, int z) {}
}
