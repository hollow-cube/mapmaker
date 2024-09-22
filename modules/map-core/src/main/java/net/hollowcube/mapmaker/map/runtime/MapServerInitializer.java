package net.hollowcube.mapmaker.map.runtime;

import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;
import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.hollowcube.mapmaker.config.MetricsConfig;
import net.hollowcube.mapmaker.config.MinestomConfig;
import net.hollowcube.mapmaker.util.MinestomPrometheus;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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
        run(serverFactory, () -> ConfigLoaderV3.loadDefault(args));
    }

    public static void run(@NotNull Function<ConfigLoaderV3, ? extends AbstractMapServer> serverFactory, @NotNull Supplier<ConfigLoaderV3> loadConfig) {
        long start = System.nanoTime();

        SYSTEM_PROPERTIES.forEach(System::setProperty);
        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // Prometheus JVM exporters
        io.prometheus.client.hotspot.DefaultExports.initialize();

        // Init tasks (minestom server, map server components, web server)

        var config = loadConfig.get();
        enablePyroscopeProfiling(config);

        var minecraftServer = MinecraftServer.init();
        MinestomPrometheus.init();
        var server = serverFactory.apply(config);

        MinecraftServer.setBrandName("minestom");
        MinecraftServer.setCompressionThreshold(-1);

        MinecraftServer.getExceptionManager().setExceptionHandler(server::handleUncaughtException);

        //todo minestom bug, need to fix
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientInteractEntityPacket.class, (packet, player) -> {
            final Entity entity = player.getInstance().getEntityById(packet.targetId());
            final double interactionRange = player.getAttributeValue(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (entity == null || !entity.isViewer(player) || player.getDistance(entity) > interactionRange)
                return;

            ClientInteractEntityPacket.Type type = packet.type();
            if (type instanceof ClientInteractEntityPacket.Attack) {
                if (entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) // Can't attack dead entities
                    return;
                EventDispatcher.call(new EntityAttackEvent(player, entity));
            } else if (type instanceof ClientInteractEntityPacket.InteractAt interactAt) {
                Point interactPosition = new Vec(interactAt.targetX(), interactAt.targetY(), interactAt.targetZ());
                EventDispatcher.call(new PlayerEntityInteractEvent(player, entity, interactAt.hand(), interactPosition));
            }
        });

        HealthObserver healthObserver = HealthObserver.builder()
                .details(true)
                .addCheck(server.shutdowner())
                .addHealthChecks(server.healthChecks())
                .build();

        ObserveFeature observe = ObserveFeature.builder()
//                .config(config.get("server.features.observe"))
                .addObserver(healthObserver)
                .build();

        var httpConfig = config.get(HttpConfig.class);
        WebServer webServer = WebServer.builder()
                .host(httpConfig.host()).port(httpConfig.port())
                .addFeature(observe)
                .routing(b -> b.register(PrometheusSupport.create().service().orElseThrow())
                        .register(server.shutdowner()))
                .build()
                .start();

        logger.info("Web server is running at {}:{}", httpConfig.host(), webServer.port());
        server.shutdowner().queue("webserver", webServer::stop);

        // Start everything

        try {
            server.start();
        } catch (Exception e) {
            logger.error("server start failed, shutting down", e);
            e.printStackTrace();
            server.shutdowner().shutdownImmediately();
            System.exit(1);
        }

        var minestomConfig = config.get(MinestomConfig.class);
        minecraftServer.start(minestomConfig.host(), minestomConfig.port());
        MinecraftServer.getBenchmarkManager().enable(Duration.ofSeconds(2));
        server.shutdowner().queue("minestom-kick", () -> {
            // todo why doesn't Minestom do this on its own?
            var connectionManager = MinecraftServer.getConnectionManager();
            for (var player : connectionManager.getOnlinePlayers()) {
                player.kick(Component.translatable("mapmaker.shutdown"));
                EventDispatcher.call(new PlayerDisconnectEvent(player));
            }
        });
        server.shutdowner().queue("minestom-stop", MinecraftServer::stopCleanly);

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }

    private static void enablePyroscopeProfiling(@NotNull ConfigLoaderV3 config) {
        var endpoint = config.get(MetricsConfig.class).pyroscopeEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) return;
        var role = System.getenv("MAPMAKER_ROLE");
        if (role == null || role.trim().isEmpty()) return;

        logger.info("Enabling Pyroscope profiling (role={}, endpoint={})", role, endpoint);
        PyroscopeAgent.start(new Config.Builder()
                .setApplicationName("mapmaker-" + role)
                .setProfilingEvent(EventType.ITIMER)
                .setFormat(Format.JFR)
                .setServerAddress(endpoint)
                .build());
    }

}
