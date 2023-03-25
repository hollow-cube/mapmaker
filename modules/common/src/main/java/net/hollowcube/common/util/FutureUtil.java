package net.hollowcube.common.util;

import com.google.common.util.concurrent.ListenableFuture;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class FutureUtil {
    private FutureUtil() {}

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> @Nullable T handleException(@NotNull Throwable t) {
        MinecraftServer.getExceptionManager().handleException(t);
        return null;
    }

    public static <T> @NotNull CompletableFuture<T> wrap(@NotNull ListenableFuture<T> future) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        future.addListener(() -> {
            try {
                completableFuture.complete(future.get());
            } catch (Exception t) {
                if (t instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                completableFuture.completeExceptionally(t);
            }
        }, Runnable::run);
        return completableFuture;
    }
}
