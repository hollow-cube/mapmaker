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
import java.util.concurrent.TimeoutException;
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

    /// The hard deadline for the whole sequence: the player drain gets [#SHUTDOWN_MAX_WAIT_MILLIS],
    /// then the hooks get the same again, after which we halt no matter what.
    private static final long SHUTDOWN_DEADLINE_MILLIS = 2 * SHUTDOWN_MAX_WAIT_MILLIS;

    @FunctionalInterface
    public interface HookFunction {
        void run() throws Exception;
    }

    record Hook(@NotNull String name, @NotNull HookFunction task) implements HookFunction {

        @Override
        public void run() throws Exception {
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
    public void queue(@NotNull String name, @NotNull HookFunction hook) {
        shutdownHooks.add(new Hook(name, hook));
    }

    public void performShutdown() {
        if (isShuttingDown) return;
        isShuttingDown = true;

        logger.info("Beginning graceful shutdown. The server will terminate in at most {} seconds.", SHUTDOWN_DEADLINE_MILLIS / 1000);

        // The sequence runs on its own thread so that we can bound it. Anything it does may block
        // indefinitely (world saves talk to the api, hooks close network resources, and submitVirtual
        // runs inline once we have marked shutdown), and a wedged sequence keeps the jvm (and the
        // server port) alive forever.
        var sequence = new Thread(this::runShutdownSequence, "shutdown-sequence");
        sequence.setDaemon(true);
        sequence.start();

        try {
            sequence.join(SHUTDOWN_DEADLINE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (sequence.isAlive()) {
            logger.error("Shutdown did not complete within {}ms, halting.", SHUTDOWN_DEADLINE_MILLIS);
            Runtime.getRuntime().halt(0);
        }
    }

    private void runShutdownSequence() {
        try {
            quiescenceFunction.get().get(SHUTDOWN_MAX_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            logger.info("Players have drained successfully, running shutdown hooks.");
        } catch (TimeoutException e) {
            logger.error("Players did not drain within {}ms, running shutdown hooks anyway.", SHUTDOWN_MAX_WAIT_MILLIS);
        } catch (ExecutionException e) {
            logger.error("Error waiting for quiescence", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        FutureUtil.markShutdown(true);
        for (var hook : shutdownHooks) {
            try {
                hook.run();
            } catch (Throwable e) {
                logger.error("Error running shutdown hook {}", hook.name(), e);
            }
        }
    }

    @Override
    public boolean healthCheck() {
        return !isShuttingDown();
    }
}
