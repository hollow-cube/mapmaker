package net.hollowcube.mapmaker.dev;

import io.helidon.health.HealthSupport;
import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.prometheus.client.hotspot.DefaultExports;
import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.hollowcube.mapmaker.config.MinestomConfig;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapPlayerDataMgmtConsumer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapServiceImpl;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PermManagerImpl;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings({"UnstableApiUsage", "FieldCanBeLocal"})
public class DevServer {
    private static final Logger logger = LoggerFactory.getLogger(DevServer.class);

    public static void main(String[] args) {
        long start = System.nanoTime();

        System.setProperty("minestom.chunk-view-distance", "16");
        System.setProperty("minestom.command.async-virtual", "true");
        System.setProperty("minestom.event.multiple-parents", "true");
        System.setProperty("minestom.experiment.pose-updates", "true");

        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Setup pyroscope profiler
        var pyroscopeEndpoint = System.getenv("MAPMAKER_PYROSCOPE_ENDPOINT");
        if (pyroscopeEndpoint != null) {
            logger.info("Enabling pyroscope profiling...");
            PyroscopeAgent.start(new io.pyroscope.javaagent.config.Config.Builder().setApplicationName("mapmaker").setProfilingEvent(EventType.ITIMER).setFormat(Format.JFR).setServerAddress(pyroscopeEndpoint).build());
        } else {
            logger.info("Skipping profiler...");
        }

        // Prometheus JVM exporters
        DefaultExports.initialize();

        // Load config
        var config = ConfigLoaderV3.loadDefault();

        // Begin server initialization
        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getExceptionManager().setExceptionHandler(t -> {
            try {
                logger.error("An uncaught exception has been handled", t);
            } catch (Exception e) {
                System.err.println("An error occurred trying to log an unhandled exception:");
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
                System.err.println("The original stacktrace is printed below:");
                //noinspection CallToPrintStackTrace
                t.printStackTrace();
            }
        });
        var server = new DevServer();

        // Add health check & metrics web server.
        var httpConfig = config.get(HttpConfig.class);
        WebServer webServer = WebServer.builder().host(httpConfig.host()).port(httpConfig.port()).addRouting(Routing.builder().register(HealthSupport.builder().webContext("alive").addLiveness(() -> HealthCheckResponse.up("mapmaker")).build()).register(HealthSupport.builder().webContext("ready").addReadiness(server.readinessChecks()).build()).register(PrometheusSupport.create()).build()).build();
        webServer.start().thenAccept(ws -> logger.info("Web server is running at {}:{}", httpConfig.host(), ws.port()));

        // Finish server initialization
        server.start(config);
        var minestomConfig = config.get(MinestomConfig.class);
        minecraftServer.start(minestomConfig.host(), minestomConfig.port());

        // Add shutdown hook for graceful shutdown
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            webServer.shutdown();
            //noinspection ResultOfMethodCallIgnored
            ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }

    private final CommandManager hubCommandManager = new CommandManagerImpl();
    private final CommandManager mapCommandManager = new CommandManagerImpl();

    private PlayerService playerService;
    private SessionService sessionService;
    private MapService mapService;
    private PermManager permManager;

    private boolean shuttingDown = false; // Used to run some things synchronously during shutdown.

    private DevHubServer hub;
    private DevMapServer maps;


    public static Pattern onlinePlayersPattern = Pattern.compile("");

    @Blocking
    public void start(@NotNull ConfigLoaderV3 config) {
        var velocityConfig = config.get(VelocityConfig.class);
        if (!velocityConfig.secret().isEmpty()) {
            logger.info("Enabling modern forwarding...");
            VelocityProxy.enable(velocityConfig.secret());
        } else {
            logger.info("Velocity not configured, using online mode...");
            MojangAuth.init();
        }

        // Start phase 1
        // Connect to low level services

        logger.info("Connecting to remote services...");

        var playerServiceUrl = System.getenv("MAPMAKER_PLAYER_SERVICE_URL");
        if (playerServiceUrl == null) playerServiceUrl = "http://localhost:9126";
        playerService = new PlayerServiceImpl(playerServiceUrl);

        var sessionServiceUrl = System.getenv("MAPMAKER_SESSION_SERVICE_URL");
        if (sessionServiceUrl == null) sessionServiceUrl = "http://localhost:9127";
        sessionService = new SessionServiceImpl(sessionServiceUrl);

        var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
        if (mapServiceUrl == null) mapServiceUrl = "http://localhost:9125";
        mapService = new MapServiceImpl(mapServiceUrl);

        var spicedbUrl = System.getenv("MAPMAKER_SPICEDB_URL");
        if (spicedbUrl == null) spicedbUrl = "localhost:50051";
        var spicedbToken = System.getenv("MAPMAKER_SPICEDB_TOKEN");
        if (spicedbToken == null) spicedbToken = "supersecretkey";
        permManager = new PermManagerImpl(spicedbUrl, spicedbToken);

        var kafkaConfig = config.get(KafkaConfig.class);
        new MapPlayerDataMgmtConsumer(kafkaConfig.bootstrapServersStr()); //todo close me
//        metricWriter = new MetricWriter(kafkaConfig.bootstrapServersStr());

        var sessionManager = new SessionManager(sessionService, playerService, kafkaConfig, false);

        // Start phase 2
        // Start hub and map server and bridge them.

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Configure command rewriter
            MinecraftServer.getConnectionManager().setPlayerProvider((uuid, username, connection) -> new CommandHandlingPlayer(uuid, username, connection) {
                @Override
                public @NotNull CommandManager getCommandManager() {
                    System.out.println("GET COMMAND MANAGER == " + (MapWorld.forPlayerOptional(this) != null));
                    return MapWorld.forPlayerOptional(this) != null ? mapCommandManager : hubCommandManager;
                }
            });

            var bridge = new DevServerBridge();

            this.hub = new DevHubServer(bridge, playerService, sessionService, mapService, permManager, sessionManager);
            this.maps = new DevMapServer(bridge, playerService, sessionService, mapService, permManager, sessionManager);
            bridge.setHubServer(hub);
            bridge.setMapServer(maps);

            scope.fork(FutureUtil.call(() -> this.hub.init(hubCommandManager, maps.inviteService(), config)));
            scope.fork(FutureUtil.call(() -> this.maps.init(config, mapCommandManager)));

            scope.join();
        } catch (Exception e) {
            logger.error("failed during startup", e);
            System.exit(1);
        }


        // Start phase 3
        // Load all facets & other misc startup tasks like setting up some events & minestom properties

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var eventHandler = MinecraftServer.getGlobalEventHandler();
            eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
            eventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::handleConfig);
            eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);
            eventHandler.addListener(PlayerDisconnectEvent.class, this::handleDisconnect);
            eventHandler.addListener(PlayerSkinInitEvent.class, this::handleSkinInit);

            MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
            MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProviderV2.translate(component);

            var packetListenerManager = MinecraftServer.getPacketListenerManager();
            var chatMessageListener = new ChatMessageListener(playerService, mapService, kafkaConfig.bootstrapServersStr());
            packetListenerManager.setPlayListener(ClientChatMessagePacket.class, chatMessageListener);

            MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
                logger.info("Graceful shutdown starting...");
                shuttingDown = true;
                chatMessageListener.close();
                hub.shutdown();
                maps.shutdown();
            });

            scope.join();
        } catch (Exception e) {
            logger.error("failed during startup", e);
            System.exit(1);
        }
    }

    public @NotNull List<HealthCheck> readinessChecks() {
        return List.of(() -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"), () -> HealthCheckResponse.up("mapmaker"));
    }

    @Blocking
    private void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        // This event is executed off the main thread, so its ok to block here.
        var player = event.getPlayer();

        try {
            var playerData = sessionService.createSession(
                    event.getPlayerUuid().toString(),
                    event.getUsername(),
                    "todo"
            );
            player.setTag(PlayerDataV2.TAG, playerData);

            var mapPlayerData = mapService.getMapPlayerData(playerData.id());
            player.setTag(MapPlayerData.TAG, mapPlayerData);
            logger.info("loaded map player data: {}", mapPlayerData);
        } catch (SessionService.UnauthorizedError ignored) {
            player.kick(Component.text("The server is currently in a closed beta.\nVisit ")
                    .append(Component.text("hollowcube.net").clickEvent(ClickEvent.openUrl("https://hollowcube.net/")))
                    .append(Component.text(" for more information.")));
        } catch (Exception e) {
            logger.error("failed to create session", e);
            player.kick(Component.text("Failed to login. Please try again later."));
        }

    }

    private void handleConfig(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();
        logger.info("config - {}", player.getUsername());

        var targetWorld = player.getTag(MapHooks.TARGET_WORLD);
        if (targetWorld == null) {
            // Spawn into hub
            event.setSpawningInstance(hub.world().instance());
            player.setRespawnPoint(new Pos(0.5, 40, 0.5, 90, 0));
            return;
        }

        // Spawn the player into the map
        var imw = Objects.requireNonNull(FutureUtil.getUnchecked(targetWorld));

        // Resend registry containing the local biomes for this map
        var registry = new HashMap<String, NBT>();
        registry.put("minecraft:chat_type", Messenger.chatRegistry());
        registry.put("minecraft:dimension_type", MinecraftServer.getDimensionTypeManager().toNBT());
        registry.put("minecraft:worldgen/biome", imw.biomes().toNBT());
        registry.put("minecraft:damage_type", DamageType.getNBT());
        player.sendPacket(new RegistryDataPacket(NBT.Compound(registry)));

        player.sendPacket(new TagsPacket(MinecraftServer.getTagManager().getTagMap()));

        event.setSpawningInstance(imw.instance());
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        logger.info("disconnect - {}", event.getPlayer().getUsername());
        Runnable task = () -> {
            var player = event.getPlayer();
            var playerData = PlayerDataV2.fromPlayer(player);

            // There is just no need to do any of this if we arent shutting down.
            if (!shuttingDown) {
                Audiences.all().sendMessage(Component.translatable("chat.player.leave", playerData.displayName()));
                rebuildOnlinePlayersRegex();
                MiscFunctionality.broadcastTabList(Audiences.all());
            }

            try {
                sessionService.deleteSession(playerData.id());
            } catch (Exception e) {
                logger.error("Failed to close session for " + playerData.id(), e);
            }
        };

        if (shuttingDown) {
            task.run();
        } else {
            Thread.startVirtualThread(task);
        }
    }

    private void handleSkinInit(PlayerSkinInitEvent event) {
        event.setSkin(PlayerSkin.fromUuid(event.getPlayer().getUuid().toString()));
    }

    private void rebuildOnlinePlayersRegex() {
        var builder = new StringBuilder();
        builder.append("(?:^|\\s)(");
        var first = true;
        for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!first) builder.append("|");
            builder.append(player.getUsername());
            first = false;
        }
        builder.append(")");
        onlinePlayersPattern = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
    }

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        // WARNING --------
        // IF YOU ADD SOMETHING HERE, IT PROBABLY ALSO NEEDS TO BE ADDED TO THE
        // SIMILAR FUNCTION IN HubServerImpl IN THE HUB BINARY MODULE.
        // WARNING --------
        if (!event.isFirstSpawn()) return;

        var player = event.getPlayer();
        var playerData = PlayerDataV2.fromPlayer(player);

        player.setTeam(CoreTeams.DEFAULT); // todo do this based on rank
        MiscFunctionality.sendBetaHeader(player);

        // Resend the skin - TODO: this is a minestom bug, it should automatically resend metadata after reconfig but this is a temp fix.
        player.sendPacket(player.getMetadataPacket());

        var targetWorld = player.getTag(MapHooks.TARGET_WORLD);
        if (targetWorld != null) {
            player.setDisplayName(playerData.displayName()); //todo this is a Minestom bug, the display name needs to be re-sent automatically.
            MiscFunctionality.broadcastTabList(Audiences.all());

            var imw = FutureUtil.getUnchecked(targetWorld);
            if (false && imw instanceof PlayingMapWorld playingMapWorld) { // joinMapState == HubToMapBridge.JoinMapState.SPECTATING
                playingMapWorld.startSpectating(player, false);
            } else {
                imw.acceptPlayer(player, true);
            }
            return;
        }

        rebuildOnlinePlayersRegex();


        var actionBar = ActionBar.forPlayer(player);
        actionBar.addProvider(MiscFunctionality::buildCurrencyDisplay);
        actionBar.addProvider(MiscFunctionality::buildExperienceBar);

        player.setDisplayName(playerData.displayName());
        Emoji.sendTabCompletions(player);
        MiscFunctionality.broadcastTabList(Audiences.all());

        Audiences.all().sendMessage(Component.translatable("chat.player.join", playerData.displayName()));
    }

}
