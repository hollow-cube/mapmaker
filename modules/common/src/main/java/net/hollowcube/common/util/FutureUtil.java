package net.hollowcube.common.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.thread.Acquirable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

public final class FutureUtil {
    public static final Executor VIRUTAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private static volatile boolean isShuttingDown = false;

    private FutureUtil() {
    }

    // todo refactor this its cursed.
    public static void markShutdown(boolean value) {
        isShuttingDown = value;
    }

    public static final Executor VIRTUAL = Executors.newVirtualThreadPerTaskExecutor();

    public static <T> Future<@Nullable T> callNow(Callable<@Nullable T> callable) {
        try {
            return CompletableFuture.completedFuture(callable.call());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // the contract here is that if
    public static <T> CompletableFuture<@Nullable T> fork(Callable<@Nullable T> callable) {
        var future = new CompletableFuture<T>();
        submitVirtual(() -> {
            try {
                // For some reason, IntelliJ seems to think `complete` takes a non-null argument
                //noinspection DataFlowIssue
                future.complete(callable.call());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static CompletableFuture<@Nullable Void> fork(Runnable runnable) {
        return fork(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> Consumer<T> virtual(Consumer<T> consumer) {
        return value -> Thread.startVirtualThread(() -> consumer.accept(value));
    }

    public static Callable<Void> call(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
            return null;
        };
    }

    public static <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
                return null;
            }
        };
    }

    public static Runnable wrapVirtual(Runnable runnable) {
        return () -> submitVirtual(runnable);
    }

    public static void submitVirtual(Runnable runnable) {
        createVirtual(runnable);
    }

    // this is non-null 99% of the time - putting @Nullable on this I think would
    // just throw up more warnings than it's worth
    public static @UnknownNullability Thread createVirtual(Runnable runnable) {
        if (isShuttingDown) {
            runnable.run();
            return null;
        }

        return Thread.startVirtualThread(() -> {
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

    @SuppressWarnings("UnstableApiUsage")
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

    @SuppressWarnings("UnstableApiUsage")
    public static void assertTickThread(@Nullable Acquirable<?> acquirable) {
        if (isUnsafeThread(acquirable)) return;
        throw new IllegalStateException("Unsafe tick thread only call on '" + Thread.currentThread().getName() + "'");
    }

    public static void assertTickThreadWarn() {
        assertTickThreadWarn(null);
    }

    @SuppressWarnings("UnstableApiUsage")
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

    /// Blocks until the end of the current tick for the entity, then runs the task _on the tick thread_ and returns.
    public static void waitForEndOfTick(Entity entity, Runnable task) {
        if (entity.isRemoved()) {
            task.run(); // Player isnt ticking, run immediately.
            return;
        }
        var future = new CompletableFuture<Void>();
        entity.scheduler().scheduleEndOfTick(() -> {
            try {
                task.run();
                // Again, for some reason, IntelliJ thinks `complete` has a not-null argument
                //noinspection DataFlowIssue
                future.complete(null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        FutureUtil.getUnchecked(future);
    }
}
