package net.hollowcube.api;

import net.hollowcube.world.creation.chunkgenerator.ChunkGeneratorType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

public interface ITemporaryWorld {
    /**
     * Unloads this world. Only calling this will not save the world!
     */
    void unloadWorld();

    /**
     * Unloads this world and runs the given Runnable when done. Only calling this will not save the world!
     * @param runnable The Runnable that gets called when done.
     */
    void unloadWorld(Runnable runnable);

    /**
     * @return Returns the underlying Minestom Instance that represents this world.
     */
    Instance getInstance();

    /**
     * @return Returns the world's ChunkGeneratorType.
     */
    ChunkGeneratorType getChunkGeneratorType();

// We should probably only include this for map maker worlds which extend normal world types
//    /**
//     * @return Returns the world's world border size
//     */
//    double getWorldBorderSize();
//
//    /**
//     * Sets a new world border size.
//     * @param worldBorderSize The new world border size.
//     */
//    void setWorldBorderSize(double worldBorderSize);

    /**
     * @return Returns the world's player spawn position.
     */
    Pos getSpawnPosition();

    /**
     * Sets a new player spawn position for this world.
     * @param spawnPosition The new spawn position.
     */
    void setSpawnPosition(Pos spawnPosition);
}
