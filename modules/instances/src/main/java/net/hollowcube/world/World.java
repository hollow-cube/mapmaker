package net.hollowcube.world;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface World {

    @NotNull String id();

    @NotNull Instance instance();

    // Management

    @NotNull CompletableFuture<@NotNull String> saveWorld();

    @NotNull CompletableFuture<Void> unloadWorld();

    default @NotNull CompletableFuture<Void> saveAndUnloadWorld() {
        return saveWorld().thenCompose(unused -> unloadWorld());
    }

}
