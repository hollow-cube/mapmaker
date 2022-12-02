package net.hollowcube.api;

import net.hollowcube.world.unload.WorldSaveResult;

import java.util.function.Consumer;

public interface IWorld extends ITemporaryWorld {
    /**
     * Saves this world.
     */
    void saveWorld();

    /**
     * Saves this world. Provides a WorldSaveResult when done.
     * @param consumer The Consumer that provides the WorldSaveResult when done.
     */
    void saveWorld(Consumer<WorldSaveResult> consumer);

    /**
     * Saves and unloads this world.
     */
    void saveAndUnloadWorld();

    /**
     * Saves and unloads this world. Provides a WorldSaveResult when done.
     * @param consumer The Consumer that provides the WorldSaveResult when done.
     */
    void saveAndUnloadWorld(Consumer<WorldSaveResult> consumer);

    /**
     * @return Returns the world's unique id.
     */
    String getId();

    /**
     * @return Returns the internal file id of the world's data that's used in SeaweedFs.
     */
    String getSeaweedFileId();

    /**
     * @return Returns the world alias name. If no alias was set this will return the world's id.
     */
    String getAlias();

    /**
     * Sets a new world alias.
     * @param alias The new alias name the world should be named.
     */
    void setAlias(String alias);
}
