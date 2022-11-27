package net.hollowcube.terraform.history;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public record MultiBlockChange(
        @NotNull AbsoluteBlockBatch batch,
        @NotNull AbsoluteBlockBatch undoBatch
) implements Change {

    @Override
    public @NotNull CompletableFuture<Void> undo(@NotNull Instance instance) {
        var future = new CompletableFuture<Void>();
        undoBatch.apply(instance, () -> future.complete(null));
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> redo(@NotNull Instance instance) {
        var future = new CompletableFuture<Void>();
        batch.apply(instance, () -> future.complete(null));
        return future;
    }
}
