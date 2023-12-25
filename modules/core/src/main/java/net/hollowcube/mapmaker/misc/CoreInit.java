package net.hollowcube.mapmaker.misc;

import io.helidon.health.HealthSupport;
import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import io.helidon.webserver.WebServer;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.hollowcube.mapmaker.config.MinestomConfig;
import net.minestom.server.MinecraftServer;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CoreInit {

    public static void logging() {
        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void metrics() {
        // Prometheus JVM exporters
        io.prometheus.client.hotspot.DefaultExports.initialize();
    }

    public interface ServerStarter {
        void start(@NotNull ConfigLoaderV3 config);
    }

    public static @NotNull ServerStarter minestom(@NotNull Logger logger) {
        System.setProperty("minestom.chunk-view-distance", "16");
        System.setProperty("minestom.command.async-virtual", "true");
        System.setProperty("minestom.event.multiple-parents", "true");
        System.setProperty("minestom.use-new-chunk-sending", "true");
        System.setProperty("minestom.experiment.pose-updates", "true");

        System.setProperty("minestom.new-chunk-sending-count-per-interval", "50");
        System.setProperty("minestom.new-chunk-sending-send-interval", "1");

        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getExceptionManager().setExceptionHandler(t -> logger.error("An uncaught exception has been handled", t));
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });

        return config -> {
            var minestomConfig = config.get(MinestomConfig.class);
            minecraftServer.start(minestomConfig.host(), minestomConfig.port());
        };
    }

    public static void webServer(@NotNull Logger logger, @NotNull ConfigLoaderV3 config, @NotNull Collection<HealthCheck> readinessChecks, @NotNull Service additionalService) {
        var httpConfig = config.get(HttpConfig.class);
        WebServer webServer = WebServer.builder().host(httpConfig.host()).port(httpConfig.port()).addRouting(Routing.builder()
                .register(HealthSupport.builder().webContext("alive")
                        .addLiveness(() -> HealthCheckResponse.up("mapmaker")).build())
                .register(HealthSupport.builder().webContext("ready")
                        .addReadiness(readinessChecks).build())
                .register(PrometheusSupport.create())
                .register(additionalService).build()).build();
        webServer.start().thenAccept(ws -> logger.info("Web server is running at {}:{}", httpConfig.host(), ws.port()));

        Runtime.getRuntime().addShutdownHook(new Thread(webServer::shutdown));
    }

    public static void genericStandaloneInit(@NotNull Logger logger, @NotNull Supplier<? extends StandaloneServer> serverConstructor) {
        long start = System.nanoTime();

        CoreInit.logging();
        CoreInit.metrics();

        var config = ConfigLoaderV3.loadDefault();
        var minestom = CoreInit.minestom(logger);

        var server = serverConstructor.get();

        CoreInit.webServer(
                logger, config, server.readinessChecks(),
                rules -> rules.get(server::handleHttpShutdown)
        );

        server.start(config);
        minestom.start(config);

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }
}
