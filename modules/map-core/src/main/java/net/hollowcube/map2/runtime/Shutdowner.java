package net.hollowcube.map2.runtime;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Handles the typical shutdown sequence for a server.
 */
public class Shutdowner implements Service, HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(Shutdowner.class);
    private static final long SHUTDOWN_MAX_WAIT_MILLIS = 5 * 1000; // 10 seconds

    private final List<Runnable> shutdownHooks = new ArrayList<>();
    private final Supplier<CompletableFuture<Void>> quiescenceFunction;
    private volatile CompletableFuture<Void> gracefulShutdownFuture = null;

    public Shutdowner(@NotNull Supplier<CompletableFuture<Void>> quiescenceFunction) {
        this.quiescenceFunction = quiescenceFunction;

        //noinspection ResultOfMethodCallIgnored
        shutdownHooks.add(() -> ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownImmediately));
    }

    public boolean isShuttingDown() {
        return gracefulShutdownFuture != null;
    }

    /**
     * Queues a hook to be run when the server is shutting down.
     *
     * <p>The last hook added will be the last one to be executed.</p>
     */
    public void queue(@NotNull Runnable hook) {
        shutdownHooks.add(hook);
    }

    /**
     * Shuts down the server gracefully, immediately. Does not give any shutdown grace period.
     */
    public void shutdownImmediately() {
        if (gracefulShutdownFuture == null) shutdownGracefully();
        gracefulShutdownFuture.complete(null);

        shutdownHooks.forEach(Runnable::run);
    }

    public void shutdownGracefully() {
        if (gracefulShutdownFuture != null) return;

        gracefulShutdownFuture = new CompletableFuture<>();
        logger.info("Beginning graceful shutdown. The server will terminate in {} seconds.", SHUTDOWN_MAX_WAIT_MILLIS / 1000);
        CompletableFuture.delayedExecutor(SHUTDOWN_MAX_WAIT_MILLIS, TimeUnit.MILLISECONDS)
                // Automatically complete the future after the timeout.
                .execute(() -> gracefulShutdownFuture.complete(null));

        // At this point we have entered the pre shutdown hook for the pod. We have a maximum of
        // SHUTDOWN_MAX_WAIT_MILLIS to remove all players from the server then we will enter
        // the shutdown hook for the jvm (receive sigterm).

        quiescenceFunction.get().thenRun(this::shutdownImmediately);
    }

    private void handleShutdownRequest(@NotNull ServerRequest request, @NotNull ServerResponse response) {
        if (gracefulShutdownFuture == null) {
            // We have not started shutting down separately, so begin shutdown
            shutdownGracefully();
        }

        // Return from the request whenever shutdown is completed.
        // As soon as we return from this request, Kubernetes will send a SIGTERM to kill the pod
        // which will forcibly remove all players and shutdown the process.
        gracefulShutdownFuture.thenRun(() -> response.status(200).send());
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/shutdown", this::handleShutdownRequest);
    }

    @Override
    public HealthCheckResponse call() {
        return gracefulShutdownFuture == null ? HealthCheckResponse.up("shutdown") : HealthCheckResponse.down("shutdown");
    }
}
