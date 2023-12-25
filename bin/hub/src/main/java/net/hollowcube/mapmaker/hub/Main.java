package net.hollowcube.mapmaker.hub;

import io.helidon.health.HealthSupport;
import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.hollowcube.mapmaker.config.MinestomConfig;
import net.minestom.server.MinecraftServer;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        long start = System.nanoTime();

        System.setProperty("minestom.chunk-view-distance", "16");
        System.setProperty("minestom.command.async-virtual", "true");
        System.setProperty("minestom.event.multiple-parents", "true");
        System.setProperty("minestom.use-new-chunk-sending", "true");
        System.setProperty("minestom.experiment.pose-updates", "true");

        System.setProperty("minestom.new-chunk-sending-count-per-interval", "50");
        System.setProperty("minestom.new-chunk-sending-send-interval", "1");

        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Prometheus JVM exporters
        DefaultExports.initialize();

        // Load config
        var config = ConfigLoaderV3.loadDefault();

        // Begin server initialization
        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getExceptionManager().setExceptionHandler(t -> logger.error("An uncaught exception has been handled", t));
        var server = new HubServerImpl();

        // Add health check & metrics web server.
        var httpConfig = config.get(HttpConfig.class);
        WebServer webServer = WebServer.builder().host(httpConfig.host()).port(httpConfig.port()).addRouting(Routing.builder()
                .register(HealthSupport.builder().webContext("alive")
                        .addLiveness(() -> HealthCheckResponse.up("mapmaker")).build())
                .register(HealthSupport.builder().webContext("ready")
                        .addReadiness(server.readinessChecks()).build())
                .register(PrometheusSupport.create())
                .register(rules -> rules.get(server::handleHttpShutdown)).build()).build();
        webServer.start().thenAccept(ws -> logger.info("Web server is running at {}:{}", httpConfig.host(), ws.port()));

        // Finish server initialization
        server.start(config);
        var minestomConfig = config.get(MinestomConfig.class);
        minecraftServer.start(minestomConfig.host(), minestomConfig.port());

        // Add shutdown hook for graceful shutdown
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            webServer.shutdown();
            ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }
}
