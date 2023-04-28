package net.hollowcube.world;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface World {

    @NotNull String id();

    @NotNull Instance instance();

    // Management

    @Blocking @NotNull String saveWorld();

    @Blocking void unloadWorld();

    default @Blocking void saveAndUnloadWorld() {
        saveWorld();
        unloadWorld();
    }

}
