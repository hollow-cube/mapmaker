package net.hollowcube.common.util;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public final class FutureUtil {
    private FutureUtil() {
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> @Nullable T handleException(@NotNull Throwable t) {
        MinecraftServer.getExceptionManager().handleException(t);
        return null;
    }

    public static <T> @NotNull Consumer<T> virtual(@NotNull Consumer<T> consumer) {
        return value -> Thread.startVirtualThread(() -> consumer.accept(value));
    }

    public static @NotNull Callable<Void> call(@NotNull Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
            return null;
        };
    }

    public static @NotNull Runnable wrapVirtual(@NotNull Runnable runnable) {
        return () -> submitVirtual(runnable);
    }

    public static void submitVirtual(@NotNull Runnable runnable) {
        Thread.startVirtualThread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertThread() {
        var thread = Thread.currentThread();
        if (thread.isVirtual()) return;
        if (thread instanceof ForkJoinWorkerThread fjwt && fjwt.getPool() == ForkJoinPool.commonPool())
            return;

        throw new IllegalStateException("Unsafe blocking call on '" + thread.getName() + "'");
    }

    @Contract("null -> null")
    public static <T> @UnknownNullability T getUnchecked(@Nullable Future<T> future) {
        if (future == null) return null;
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
