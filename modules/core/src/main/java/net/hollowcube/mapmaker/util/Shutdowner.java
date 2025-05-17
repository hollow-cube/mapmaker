package net.hollowcube.mapmaker.util;

import net.hollowcube.common.util.FutureUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Handles the typical shutdown sequence for a server.
 */
public class Shutdowner implements HttpServerWrapper.HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(Shutdowner.class);
    private static final long SHUTDOWN_MAX_WAIT_MILLIS;

    static {
        try {
            SHUTDOWN_MAX_WAIT_MILLIS = Long.parseLong(System.getenv().getOrDefault("MAPMAKER_SHUTDOWN_MAX_WAIT_MILLIS", "5000"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("SHUTDOWN_MAX_WAIT_MILLIS must be a valid number", e);
        }
    }

    record Hook(@NotNull String name, @NotNull Runnable task) implements Runnable {

        @Override
        public void run() {
            task.run();
        }
    }

    private final List<Hook> shutdownHooks = new ArrayList<>();
    private final Supplier<CompletableFuture<Void>> quiescenceFunction;

    private volatile boolean isShuttingDown = false;

    public Shutdowner(@NotNull Supplier<CompletableFuture<Void>> quiescenceFunction) {
        this.quiescenceFunction = quiescenceFunction;

        //noinspection ResultOfMethodCallIgnored
        shutdownHooks.add(new Hook("fjp wait", () -> ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS)));

        Runtime.getRuntime().addShutdownHook(new Thread(this::performShutdown));
    }

    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    /**
     * Queues a hook to be run when the server is shutting down.
     *
     * <p>The last hook added will be the last one to be executed.</p>
     */
    public void queue(@NotNull String name, @NotNull Runnable hook) {
        shutdownHooks.add(new Hook(name, hook));
    }

    public void performShutdown() {
        if (isShuttingDown) return;

        try {
            logger.info("Beginning graceful shutdown. The server will terminate in {} seconds.", SHUTDOWN_MAX_WAIT_MILLIS / 1000);
            try {
                quiescenceFunction.get().get();
            } catch (ExecutionException e) {
                logger.error("Error waiting for quiescence", e);
            }

            logger.info("Players have drained successfully, running shutdown hooks.");
            FutureUtil.markShutdown(true);
            for (var hook : shutdownHooks) {
                try {
                    hook.run();
                } catch (Throwable e) {
                    logger.error("Error running shutdown hook {}", hook.name(), e);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean healthCheck() {
        return !isShuttingDown();
    }
}
