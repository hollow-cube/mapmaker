package net.hollowcube.mapmaker.util;

import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import net.hollowcube.common.util.FutureUtil;
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
public class Shutdowner implements HttpService, HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(Shutdowner.class);
    private static final long SHUTDOWN_MAX_WAIT_MILLIS;

    private volatile boolean isShuttingDown = false;

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
    private volatile CompletableFuture<Void> gracefulShutdownFuture = null;

    public Shutdowner(@NotNull Supplier<CompletableFuture<Void>> quiescenceFunction) {
        this.quiescenceFunction = quiescenceFunction;

        //noinspection ResultOfMethodCallIgnored
        shutdownHooks.add(new Hook("fjp wait", () -> ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS)));

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
    public void queue(@NotNull String name, @NotNull Runnable hook) {
        shutdownHooks.add(new Hook(name, hook));
    }

    /**
     * Shuts down the server gracefully, immediately. Does not give any shutdown grace period.
     */
    public void shutdownImmediately() {
        if (isShuttingDown) return;
        if (gracefulShutdownFuture == null) shutdownGracefully();
        gracefulShutdownFuture.complete(null);
        FutureUtil.markShutdown();

        isShuttingDown = true;
        shutdownHooks.forEach(runnable -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void shutdownGracefully() {
        if (gracefulShutdownFuture != null) return;

        logger.info("Beginning graceful shutdown. The server will terminate in {} seconds.", SHUTDOWN_MAX_WAIT_MILLIS / 1000);
        gracefulShutdownFuture = CompletableFuture.runAsync(
                () -> {
                }, // Automatically complete the future after the timeout.
                CompletableFuture.delayedExecutor(SHUTDOWN_MAX_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        );

        // At this point we have entered the pre shutdown hook for the pod. We have a maximum of
        // SHUTDOWN_MAX_WAIT_MILLIS to remove all players from the server then we will enter
        // the shutdown hook for the jvm (receive sigterm).

        quiescenceFunction.get().thenRun(this::shutdownImmediately);
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/shutdown", this::handleShutdownRequest);
    }

    private void handleShutdownRequest(@NotNull ServerRequest request, @NotNull ServerResponse response) {
        if (gracefulShutdownFuture == null) {
            // We have not started shutting down separately, so begin shutdown
            shutdownGracefully();
        }

        // Return from the request whenever shutdown is completed.
        // As soon as we return from this request, Kubernetes will send a SIGTERM to kill the pod
        // which will forcibly remove all players and shutdown the process.
        FutureUtil.getUnchecked(gracefulShutdownFuture);
        response.status(200).send();
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.builder().status(gracefulShutdownFuture == null).build();
    }

    @Override
    public HealthCheckType type() {
        return HealthCheckType.READINESS;
    }

    @Override
    public String name() {
        return "shutdowner";
    }
}
