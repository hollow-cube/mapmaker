package net.hollowcube.common.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.thread.Acquirable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

public final class FutureUtil {
    private static volatile boolean isShuttingDown = false;

    private FutureUtil() {
    }

    // todo refactor this its cursed.
    public static void markShutdown(boolean value) {
        isShuttingDown = value;
    }

    public static final Executor VIRTUAL = Executors.newVirtualThreadPerTaskExecutor();

    public static <T> @NotNull Future<T> callNow(@NotNull Callable<T> callable) {
        try {
            return CompletableFuture.completedFuture(callable.call());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public static <T> @NotNull CompletableFuture<T> fork(@NotNull Callable<T> callable) {
        var future = new CompletableFuture<T>();
        submitVirtual(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static @NotNull CompletableFuture<Void> fork(@NotNull Runnable runnable) {
        return fork(() -> {
            runnable.run();
            return null;
        });
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

    public static <T> @NotNull Callable<T> wrap(@NotNull Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
                return null;
            }
        };
    }

    public static @NotNull Runnable wrapVirtual(@NotNull Runnable runnable) {
        return () -> submitVirtual(runnable);
    }

    public static void submitVirtual(@NotNull Runnable runnable) {
        if (isShuttingDown) {
            runnable.run();
            return;
        }

        Thread.startVirtualThread(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

    public static void sleep(long millis) {
        FutureUtil.assertThreadWarn();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isUnsafeThread(@Nullable Acquirable<?> acquirable) {
        if (isShuttingDown) return false;
        var thread = Thread.currentThread();
        if (acquirable != null && !acquirable.isLocal() && acquirable.assignedThread() != null && !acquirable.assignedThread().lock().isHeldByCurrentThread())
            return true; // Above means: We have acquirable, we are not on its thread, and we do not hold the lock of its thread
        if (thread.isVirtual()) return false;
        if (thread instanceof ForkJoinWorkerThread fjwt && fjwt.getPool() == ForkJoinPool.commonPool())
            return false;

        return true;
    }

    public static void assertThread() {
        if (!isUnsafeThread(null)) return;
        throw new IllegalStateException("Unsafe blocking call on '" + Thread.currentThread().getName() + "'");
    }

    private static final Logger logger = LoggerFactory.getLogger(FutureUtil.class);

    public static void assertThreadWarn() {
        if (!isUnsafeThread(null)) return;

        logger.error("Unsafe blocking call on '{}'", Thread.currentThread().getName(), new RuntimeException("dummy exception for stacktrace"));
    }

    public static void assertTickThread() {
        assertTickThread(null);
    }

    public static void assertTickThread(@Nullable Acquirable<?> acquirable) {
        if (isUnsafeThread(acquirable)) return;
        throw new IllegalStateException("Unsafe tick thread only call on '" + Thread.currentThread().getName() + "'");
    }

    public static void assertTickThreadWarn() {
        assertTickThreadWarn(null);
    }

    public static void assertTickThreadWarn(@Nullable Acquirable<?> acquirable) {
        if (isUnsafeThread(acquirable)) return;
        logger.error("Unsafe tick thread only call on '{}'", Thread.currentThread().getName(), new RuntimeException("dummy exception for stacktrace"));
    }

    @Contract("null -> null")
    public static <T> @UnknownNullability T getUnchecked(@Nullable Future<T> future) {
        FutureUtil.assertThreadWarn();
        if (future == null) return null;
        try {
            return future.get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Contract("null, _ -> null")
    public static <T> @UnknownNullability T getUnchecked(@Nullable Future<T> future, long timeoutMillis) {
        FutureUtil.assertThreadWarn();
        if (future == null) return null;
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignored) {
            return null; // Timeout is expected, return null
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
