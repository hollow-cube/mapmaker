package net.hollowcube.mapmaker.instance.generation;

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

public class VoidGenerator implements Generator {

    private static final int PLATFORM_SIZE = 3;
    private final Map<ChunkCoordinatePair, List<Point>> blocksToSet = new HashMap<>();

    public VoidGenerator() {
        int bounds = PLATFORM_SIZE / 2;
        for (int x = -bounds; x <= bounds; x++) {
            for (int z = -bounds; z <= bounds; z++) {
                ChunkCoordinatePair pair = new ChunkCoordinatePair(
                        (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X),
                        (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z)
                );
                blocksToSet.putIfAbsent(pair, new ArrayList<>());
                blocksToSet.get(pair).add(new Pos(x, 39, z));
            }
        }
    }

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Place all points we precalculated earlier
        var pair = new ChunkCoordinatePair(unit.absoluteStart().chunkX(), unit.absoluteStart().chunkZ());
        if (blocksToSet.containsKey(pair)) {
            for (Point point : blocksToSet.get(pair)) {
                unit.modifier().setBlock(point, Block.STONE);
            }
        }
    }

    private record ChunkCoordinatePair(int x, int z) {}
}
