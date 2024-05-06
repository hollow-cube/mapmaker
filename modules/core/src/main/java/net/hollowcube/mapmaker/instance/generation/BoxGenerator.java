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
    private final static int MIN_X = 0;
    private final static int MAX_X = 14;
    private final static int MIN_Z = 0;
    private final static int MAX_Z = 14;
    private static final int BOX_HEIGHT = 21;
    private static final int BOTTOM_HEIGHT = 36;
    private static final int TOP_HEIGHT = BOTTOM_HEIGHT + BOX_HEIGHT;
    
    private final Map<ChunkCoordinatePair, List<Point>> blocksToSet = new HashMap<>();

    public BoxGenerator(BoxType type) {

        // Generate the box TODO use structures instead
        for (int x = MIN_X - 1; x <= MAX_X + 1; x++) {
            for (int z = MIN_Z - 1; z <= MAX_Z + 1; z++) {
                for (int y = BOTTOM_HEIGHT; y < TOP_HEIGHT; y++) {
                    BoxGenerator.ChunkCoordinatePair pair = new BoxGenerator.ChunkCoordinatePair(
                            (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X),
                            (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z)
                    );
                    blocksToSet.putIfAbsent(pair, new ArrayList<>());

                    // Always place the corners
                    if ((x == MIN_X - 1 || x == MAX_X + 1) && (z == MIN_Z - 1 || z == MAX_Z + 1))
                        blocksToSet.get(pair).add(new Pos(x, y, z));

                    // If at bottom or top of box, place square
                    if ((y == BOTTOM_HEIGHT || y == TOP_HEIGHT - 1) &&
                            (x == MIN_X - 1 || x == MAX_X + 1 || z == MIN_Z - 1 || z == MAX_Z + 1))
                        blocksToSet.get(pair).add(new Pos(x, y, z));
                }
            }
        }

        // Generate start platform
        for (int x = (MIN_X + MAX_X) / 2 - 1; x <= (MIN_X + MAX_X) / 2 + 1; x++) {
            for (int z = MIN_Z - 3; z <= MIN_Z - 1; z++) {
                BoxGenerator.ChunkCoordinatePair pair = new BoxGenerator.ChunkCoordinatePair(
                        (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X),
                        (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z)
                );
                blocksToSet.putIfAbsent(pair, new ArrayList<>());

                blocksToSet.get(pair).add(new Pos(x, BOTTOM_HEIGHT + 3, z));
            }
        }

        // Generate end platform
        for (int x = (MIN_X + MAX_X) / 2 - 1; x <= (MIN_X + MAX_X) / 2 + 1; x++) {
            for (int z = MAX_Z + 1; z <= MAX_Z + 3; z++) {
                BoxGenerator.ChunkCoordinatePair pair = new BoxGenerator.ChunkCoordinatePair(
                        (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X),
                        (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z)
                );
                blocksToSet.putIfAbsent(pair, new ArrayList<>());

                blocksToSet.get(pair).add(new Pos(x, BOTTOM_HEIGHT + 3, z));
            }
        }

        // TODO support corner boxes
//        for (int x = MAX_X + 1; x <= MAX_X + 3; x++) {
//            for (int z = (MIN_Z + MAX_Z) / 2 - 1; z <= (MIN_Z + MAX_Z) / 2 + 1; z++) {
//                BoxGenerator.ChunkCoordinatePair pair = new BoxGenerator.ChunkCoordinatePair(
//                        (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X),
//                        (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z)
//                );
//                blocksToSet.putIfAbsent(pair, new ArrayList<>());
//
//                blocksToSet.get(pair).add(new Pos(x, BOTTOM_HEIGHT + 3, z));
//            }
//        }
    }

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Place all points we precalculated earlier
        // TODO use bounding box from structures instead
        var pair = new BoxGenerator.ChunkCoordinatePair(unit.absoluteStart().chunkX(), unit.absoluteStart().chunkZ());
        if (blocksToSet.containsKey(pair)) {
            for (Point point : blocksToSet.get(pair)) {
                unit.modifier().setBlock(point, Block.STONE);
            }
        }
    }

    private record ChunkCoordinatePair(int x, int z) {}
}
