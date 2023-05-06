package net.hollowcube.world;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public interface World {

    @NotNull String id();

    @NotNull Instance instance();

    // Management

    @Blocking
    @NotNull String saveWorld();

    @Blocking
    void unloadWorld();

    default @Blocking void saveAndUnloadWorld() {
        saveWorld();
        unloadWorld();
    }

}
