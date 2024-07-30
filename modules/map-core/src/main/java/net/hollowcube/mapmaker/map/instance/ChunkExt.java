package net.hollowcube.mapmaker.map.instance;

import net.minestom.server.coordinate.Point;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

/**
 * Accessor for extra fields on a chunk, regardless of whether the chunk is internally a lighting chunk or not.
 * I hate inheritance. Composition >>>>>>>>
 */
public interface ChunkExt {

    int getHeight(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap, int x, int z);
    default int getHeight(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap, @NotNull Point point) {
        return getHeight(heightmap, point.blockX(), point.blockZ());
    }

    @NotNull Heightmap heightmap(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap);

    void loadHeightmap(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap, int[] data);
    int[] saveHeightmap(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap);

}
