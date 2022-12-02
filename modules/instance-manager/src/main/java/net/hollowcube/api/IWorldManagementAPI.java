package net.hollowcube.api;

import net.hollowcube.WorldManager;
import net.hollowcube.world.load.WorldLoadResult;
import net.hollowcube.world.unload.WorldSaveResult;

import java.util.function.Consumer;

public interface IWorldManagementAPI {
    /**
     * Tries to load the specified world.
     * @param id The id of the world.
     */
    void loadWorld(String id);

    /**
     * Tries to load the specified world. Provides a WorldLoadResult when done.
     * @param id The id of the world.
     * @param consumer The Consumer that provides the WorldLoadResult when done.
     */
    void loadWorld(String id, Consumer<WorldLoadResult> consumer);

    /**
     * Tries to unload the world.
     * @param id The id of the world.
     */
    void unloadWorld(String id);

    /**
     * Tries to unload the world.
     * @param world The world object you want to unload.
     */
    void unloadWorld(ITemporaryWorld world);

    /**
     * Tries to unload the world and runs the given Runnable when done.
     * @param id The id of the world.
     * @param runnable The Runnable that gets called when done.
     */
    void unloadWorld(String id, Runnable runnable);

    /**
     * Tries to unload the world and runs the given Runnable when done.
     * @param world The world object you want to unload.
     * @param runnable The Runnable that gets called when done.
     */
    void unloadWorld(ITemporaryWorld world, Runnable runnable);

    /**
     * Tries to save the world. If you specify a TemporaryWorld this operation will fail.
     * @param id The id of the world.
     */
    void saveWorld(String id);

    /**
     * Tries to save the world.
     * @param world The world object you want to save.
     */
    void saveWorld(IWorld world);

    /**
     * Tries to save the world. Provides a WorldSaveResult when done. If you specify a TemporaryWorld this operation will fail.
     * @param id The id of the world
     * @param consumer The Consumer that provides the WorldSaveResult when done.
     */
    void saveWorld(String id, Consumer<WorldSaveResult> consumer);

    /**
     * Tries to save the world. Provides a WorldSaveResult when done.
     * @param world The world object you want to save.
     * @param consumer The Consumer that provides the WorldSaveResult when done.
     */
    void saveWorld(IWorld world, Consumer<WorldSaveResult> consumer);

    /**
     * Tries to save and unload the world. If you specify a TemporaryWorld this operation will fail.
     * @param id The id of the world.
     */
    void saveAndUnloadWorld(String id);

    /**
     * Tries to save and unload the world.
     * @param world The world object you want to save and unload.
     */
    void saveAndUnloadWorld(IWorld world);

    /**
     * Tries to save and unload the world. Provides a WorldSaveResult when done. If you specify a TemporaryWorld this operation will fail.
     * @param id The id of the world
     * @param consumer The Consumer that provides the WorldSaveResult when done.
     */
    void saveAndUnloadWorld(String id, Consumer<WorldSaveResult> consumer);

    /**
     * Tries to save and unload the world. Provides a WorldSaveResult when done.
     * @param world The world object you want to save and unload.
     * @param consumer The Consumer that provides the WorldSaveResult when done.
     */
    void saveAndUnloadWorld(IWorld world, Consumer<WorldSaveResult> consumer);

    /**
     * @return Returns the WorldManager instance.
     */
    WorldManager getWorldManager();
}
