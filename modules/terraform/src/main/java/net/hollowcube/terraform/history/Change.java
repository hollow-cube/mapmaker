package net.hollowcube.terraform.history;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface Change {

    @NotNull CompletableFuture<Void> undo(@NotNull Instance instance);

    @NotNull CompletableFuture<Void> redo(@NotNull Instance instance);

}
