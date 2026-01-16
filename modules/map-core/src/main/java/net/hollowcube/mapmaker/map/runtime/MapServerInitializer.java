package net.hollowcube.mapmaker.map.runtime;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.hollowcube.mapmaker.config.MinestomConfig;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.util.HttpServerWrapper;
import net.hollowcube.mapmaker.util.telemetry.CustomJVMPrometheus;
import net.hollowcube.mapmaker.util.telemetry.MinestomPrometheus;
import net.hollowcube.posthog.PostHog;
import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
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
    public static final Map<String, String> SYSTEM_PROPERTIES = Map.of(
        "minestom.chunk-view-distance", "16",
        "minestom.command.async-virtual", "true",
        "minestom.event.multiple-parents", "true",
        "minestom.shutdown-on-signal", "false", // We have our own shutdown logic which will call stopCleanly
        "minestom.new-socket-write-lock", "true"
    );

    public static MinecraftServer preInitializedServer;

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
        // Default thread death handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            PostHog.captureException(e, null, (Object) Map.of("thread", t.getName()));
            logger.error("Uncaught exception in thread {}", t, e);
        });

        // Init tasks (minestom server, map server components, web server)

        var config = loadConfig.get();

        FutureUtil.markShutdown(true);

        MinecraftServer minecraftServer = preInitializedServer;
        if (minecraftServer == null) {
            Auth auth;
            var velocityConfig = config.get(VelocityConfig.class);
            if (!velocityConfig.secret().isEmpty()) {
                auth = new Auth.Velocity(velocityConfig.secret());
            } else {
                auth = new Auth.Online();
            }

            minecraftServer = MinecraftServer.init(auth);
        }

        CustomJVMPrometheus.init();
        MinestomPrometheus.init();
        var ignored = DimensionTypes.FULL_BRIGHT; // Force initialization
        var server = serverFactory.apply(config);

        MinecraftServer.setBrandName("minestom");
        MinecraftServer.setCompressionThreshold(-1);

        MinecraftServer.getExceptionManager().setExceptionHandler(t -> {
            logger.error("An uncaught exception has been handled", t);
            PostHog.captureException(t);
        });

        //todo minestom bug, need to fix
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientInteractEntityPacket.class, (packet, player) -> {
            final Entity entity = player.getInstance().getEntityById(packet.targetId());
            final double interactionRange = player.getAttributeValue(Attribute.ENTITY_INTERACTION_RANGE);
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

        var httpConfig = config.get(HttpConfig.class);
        var httpServer = new HttpServerWrapper(httpConfig);
        httpServer.addRoute("/metrics", new HTTPServer.HTTPMetricHandler(CollectorRegistry.defaultRegistry));
        httpServer.addRoute("/alive", new HttpServerWrapper.AliveHttpHandler());
        httpServer.addRoute("/ready", new HttpServerWrapper.ReadyHttpHandler(server.healthChecks()));
        httpServer.addRoute("/players", new HttpServerWrapper.PlayerStatusHandler());
        httpServer.start();

        logger.info("Web server is running at {}:{}", httpConfig.host(), httpServer.port());
        // We add the web server shutdown task last so that it is the last thing to shut down.

        try {
            server.start();
        } catch (Exception e) {
            logger.error("server start failed, shutting down", e);
            server.shutdowner().performShutdown();
            httpServer.shutdown();
            System.exit(1);
        }

        FutureUtil.markShutdown(false);

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

        server.shutdowner().queue("http-server", httpServer::shutdown);
        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }

}
