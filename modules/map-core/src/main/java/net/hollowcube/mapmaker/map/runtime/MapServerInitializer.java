package net.hollowcube.mapmaker.map.runtime;

import io.helidon.health.HealthSupport;
import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.hollowcube.mapmaker.config.MinestomConfig;
import net.hollowcube.mapmaker.util.MinestomPrometheus;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"ResultOfMethodCallIgnored", "UnstableApiUsage"})
public final class MapServerInitializer {
    private static final Logger logger = LoggerFactory.getLogger(MapServerInitializer.class);
    private static final Map<String, String> SYSTEM_PROPERTIES = Map.of(
            "minestom.chunk-view-distance", "16",
            "minestom.command.async-virtual", "true",
            "minestom.event.multiple-parents", "true",
            "minestom.experiment.pose-updates", "true",
            "minestom.shutdown-on-signal", "false" // We have our own shutdown logic which will call stopCleanly
    );

    public static void run(@NotNull Function<ConfigLoaderV3, ? extends AbstractMapServer> serverFactory, @NotNull String[] args) {
        long start = System.nanoTime();

        SYSTEM_PROPERTIES.forEach(System::setProperty);
        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // Prometheus JVM exporters
        io.prometheus.client.hotspot.DefaultExports.initialize();

        // Init tasks (minestom server, map server components, web server)

        var config = ConfigLoaderV3.loadDefault(args);

        var minecraftServer = MinecraftServer.init();
        MinestomPrometheus.init();
        var server = serverFactory.apply(config);

        MinecraftServer.setBrandName("minestom");
        MinecraftServer.setCompressionThreshold(-1);

        MinecraftServer.getExceptionManager().setExceptionHandler(server::handleUncaughtException);

        var httpConfig = config.get(HttpConfig.class);
        WebServer webServer = WebServer.builder().host(httpConfig.host()).port(httpConfig.port()).addRouting(Routing.builder()
                .register(HealthSupport.builder().webContext("alive")
                        .addLiveness(() -> HealthCheckResponse.up("mapmaker")).build())
                .register(HealthSupport.builder().webContext("ready")
                        .addReadiness(server.readinessChecks()).build())
                .register(PrometheusSupport.create())
                .register(server.shutdowner()).build()).build();

        // Start everything

        webServer.start().thenAccept(ws -> logger.info("Web server is running at {}:{}", httpConfig.host(), ws.port()));
        server.shutdowner().queue(webServer::shutdown);

        server.start();

        var minestomConfig = config.get(MinestomConfig.class);
        minecraftServer.start(minestomConfig.host(), minestomConfig.port());
        MinecraftServer.getBenchmarkManager().enable(Duration.ofSeconds(2));
        server.shutdowner().queue(() -> {
            // todo why doesn't Minestom do this on its own?
            var connectionManager = MinecraftServer.getConnectionManager();
            for (var player : connectionManager.getOnlinePlayers()) {
                player.kick(Component.translatable("mapmaker.shutdown"));
                EventDispatcher.call(new PlayerDisconnectEvent(player));
            }
        });
        server.shutdowner().queue(MinecraftServer::stopCleanly);

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }

}
