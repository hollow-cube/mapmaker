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
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.dev.command.DebugCommand;
import net.hollowcube.mapmaker.dev.command.ToggleScoreboardCommand;
import net.hollowcube.mapmaker.dev.config.Config;
import net.hollowcube.mapmaker.dev.config.NewConfigProvider;
import net.hollowcube.mapmaker.dev.http.HttpConfig;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapServiceImpl;
import net.hollowcube.mapmaker.map.MapServiceMemory;
import net.hollowcube.mapmaker.player.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.item.Material;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class DevServer {
    private static final System.Logger logger = System.getLogger(DevServer.class.getName());

    public static void main(String[] args) {
        long start = System.nanoTime();

        System.setProperty("minestom.chunk-view-distance", "16");
        System.setProperty("minestom.command.async-virtual", "true");
        System.setProperty("minestom.event.multiple-parents", "true");
        System.setProperty("minestom.use-new-chunk-sending", "true");

        System.setProperty("minestom.new-chunk-sending-count-per-interval", "50");
        System.setProperty("minestom.new-chunk-sending-send-interval", "1");

        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Setup pyroscope profiler
        var pyroscopeEndpoint = System.getenv("MAPMAKER_PYROSCOPE_ENDPOINT");
        if (pyroscopeEndpoint != null) {
            logger.log(System.Logger.Level.INFO, "Enabling pyroscope profiling...");
            PyroscopeAgent.start(new io.pyroscope.javaagent.config.Config.Builder().setApplicationName("mapmaker").setProfilingEvent(EventType.ITIMER).setFormat(Format.JFR).setServerAddress(pyroscopeEndpoint).build());
        } else {
            logger.log(System.Logger.Level.INFO, "Skipping profiler...");
        }

        // Prometheus JVM exporters
        DefaultExports.initialize();

        // Load config
        Path configPath = Path.of("config.yaml");
        var config = Config.loadFromFile(configPath);
        var configProvider = NewConfigProvider.loadFromFile(configPath);

        // Begin server initialization
        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getExceptionManager().setExceptionHandler(t -> logger.log(System.Logger.Level.ERROR, "An uncaught exception has been handled", t));
        var server = new DevServer();

        // Add health check & metrics web server.
        var httpConfig = configProvider.get(HttpConfig.class);
        WebServer webServer = WebServer.builder().host(httpConfig.host()).port(httpConfig.port()).addRouting(Routing.builder().register(HealthSupport.builder().webContext("alive").addLiveness(() -> HealthCheckResponse.up("mapmaker")).build()).register(HealthSupport.builder().webContext("ready").addReadiness(server.readinessChecks()).build()).register(PrometheusSupport.create()).build()).build();
        webServer.start().thenAccept(ws -> logger.log(System.Logger.Level.INFO, "Web server is running at " + config.http().host() + ":" + ws.port()));

        // Finish server initialization
        server.start(config, configProvider);
        minecraftServer.start(config.minestom().host(), config.minestom().port());

        // Add shutdown hook for graceful shutdown
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            webServer.shutdown();
            ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });

        logger.log(System.Logger.Level.INFO, "Server started in {0}ms", (System.nanoTime() - start) / 1_000_000);
    }

    private PlayerService playerService;
    private SessionService sessionService;
    private MapService mapService;

    private DevHubServer hub;
    private DevMapServer maps;

    @Blocking
    public void start(@NotNull Config config, @NotNull NewConfigProvider configProvider) {
        var velocitySecret = System.getenv("MAPMAKER_VELOCITY_SECRET");
        if (velocitySecret != null) {
            logger.log(System.Logger.Level.INFO, "Enabling velocity proxy...");
            VelocityProxy.enable(velocitySecret);
        } else {
            logger.log(System.Logger.Level.INFO, "Velocity not configured, using online mode...");
            MojangAuth.init();
        }

        // Start phase 1
        // Connect to low level services

        if ("1".equals(System.getenv("MAPMAKER_STANDALONE"))) {
            logger.log(System.Logger.Level.INFO, "Using standalone stubs...");

            playerService = new PlayerServiceMemory();
            sessionService = new SessionServiceMemory((PlayerServiceMemory) playerService);
            mapService = new MapServiceMemory();
        } else {
            logger.log(System.Logger.Level.INFO, "Connecting to remote services...");

            var playerServiceUrl = System.getenv("MAPMAKER_PLAYER_SERVICE_URL");
            if (playerServiceUrl == null) playerServiceUrl = "http://localhost:9126";
            playerService = new PlayerServiceImpl(playerServiceUrl);

            var sessionServiceUrl = System.getenv("MAPMAKER_SESSION_SERVICE_URL");
            if (sessionServiceUrl == null) sessionServiceUrl = "http://localhost:9127";
            sessionService = new SessionServiceImpl(sessionServiceUrl);

            var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
            if (mapServiceUrl == null) mapServiceUrl = "http://localhost:9125";
            mapService = new MapServiceImpl(mapServiceUrl);
        }

        // Start phase 2
        // Start hub and map server and bridge them.

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var bridge = new DevServerBridge();

            this.hub = new DevHubServer(bridge, playerService, sessionService, mapService);
            this.maps = new DevMapServer(bridge, playerService, sessionService, mapService);
            bridge.setHubServer(hub);
            bridge.setMapServer(maps);

            scope.fork(FutureUtil.call(this.hub::init));
            scope.fork(FutureUtil.call(() -> this.maps.init(configProvider)));

            scope.join();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
            System.exit(1);
        }


        // Start phase 3
        // Load all facets & other misc startup tasks like setting up some events & minestom properties

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            MinecraftServer.getCommandManager().register(new DebugCommand(playerService));
            MinecraftServer.getCommandManager().register(new ToggleScoreboardCommand());

            var eventHandler = MinecraftServer.getGlobalEventHandler();
            eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
            eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
            eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);
            eventHandler.addListener(PlayerDisconnectEvent.class, this::handleDisconnect);
            eventHandler.addListener(MapDeletedEvent.class, event -> {
                for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                    var playerData = PlayerDataV2.fromPlayer(player);
                    for (int i = 0; i < playerData.getUnlockedMapSlots(); i++) {
                        var map = playerData.getMapSlot(i);
                        if (map != null && map.equals(event.mapId())) {
                            logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was deleted.", event.mapId(), playerData.id());
                            playerData.setMapSlot(i, null);
                        }
                    }
                }
            });

            MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
            MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProvider.get2(component);

            int i = 0;
            for (var facet : ServiceLoader.load(Facet.class)) {
                scope.fork(Executors.callable(() -> facet.hook(MinecraftServer.process(), configProvider)));
                i++;
            }
            logger.log(System.Logger.Level.INFO, "Loaded {0} facets.", i);

//            Scoreboards.init();
//            TabLists.init();

            MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
                logger.log(System.Logger.Level.INFO, "Graceful shutdown starting...");
                hub.shutdown();
                maps.shutdown();
            });

            scope.join();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
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
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed to create session", e);
            player.kick(Component.text("Failed to login. Please try again later."));
        }

        // todo this cleanup step should probably be moved
        // Cleanup maps which are actually gone
        Thread.startVirtualThread(() -> {
            try {
                var playerData = PlayerDataV2.fromPlayer(player);
                boolean changed = false;
                for (int i = 0; i < playerData.getUnlockedMapSlots(); i++) {
                    var mapId = playerData.getMapSlot(i);
                    if (mapId != null) {
                        try {
                            var map = mapService.getMap(playerData.id(), mapId);
                            if (map.isPublished()) {
                                // Map is published, delete from slots.
                                changed = true;
                                playerData.setMapSlot(i, null);
                                logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was published.", mapId, playerData.id());
                            }
                        } catch (MapService.NotFoundError e) {
                            // Map is gone, delete from slots
                            changed = true;
                            playerData.setMapSlot(i, null);
                            logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was deleted.", mapId, playerData.id());
                        } catch (Exception e) {
                            logger.log(System.Logger.Level.ERROR, "Failed to load map data for " + event.getUsername(), e);
                            player.kick(Component.text("Failed to load map data! Please try again later."));
                            return;
                        }
                    }
                }
                if (changed) {
                    var req = new PlayerDataUpdateRequest().setMapSlots(playerData.getRawMapSlots());
                    playerService.updatePlayerData(playerData.id(), req);
                }
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "failed to cleanup player data", e);
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

    private void handleLogin(PlayerLoginEvent event) {
        event.setSpawningInstance(hub.world().instance());
        event.getPlayer().setRespawnPoint(new Pos(0.5, 40, 0.5, 90, 0));
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        Thread.startVirtualThread(() -> {
            var player = event.getPlayer();
            var playerData = PlayerDataV2.fromPlayer(player);

            Audiences.all().sendMessage(Component.translatable("chat.player.leave", playerData.displayName()));

            try {
                //todo we may want a dead letter or something, but im not sure where to put it. This requires a lot more thought
                sessionService.deleteSession(playerData.id());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to close session for " + playerData.id(), e);
            }
        });
    }

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        //todo this gamemode/fly/permission level stuff should be handled by the hub server
        var player = event.getPlayer();
//        player.setGameMode(GameMode.CREATIVE);
//        player.setAllowFlying(true);
//        player.setPermissionLevel(4);
//        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.5f);

        var playerData = PlayerDataV2.fromPlayer(player);
        Audiences.all().sendMessage(Component.translatable("chat.player.join", playerData.displayName()));

        // Alpha watermark
        var runtime = ServerRuntime.getRuntime();
        String watermarkString = String.format("MapMaker %s+%s, Not representative of final product", runtime.version(), runtime.commit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString).color(TextColor.color(78, 92, 36)), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

//        Scoreboards.showPlayerLobbyScoreboard(player);
//        Scoreboards.setScoreboardVisibility(player, Boolean.TRUE);
//        TabLists.showPlayerGlobalTabList(player);

        Thread.startVirtualThread(() -> {
            if (System.getenv("MAPMAKER_AUTOCREATE_PUBLISHED") != null) {
                if (playerData.getSlotState(0) != SlotState.EMPTY)
                    return;

                var map = hub.handler().createMapForPlayerInSlot(playerData, 0);
                map.settings().setName("Auto Created Map");
                map.settings().setIcon(Material.STONE);
                mapService.updateMap(playerData.id(), map.id(), map.settings().getUpdateRequest());
                mapService.publishMap(playerData.id(), map.id());

            }
            if (System.getenv("MAPMAKER_AUTOEDIT_MAP") != null) {

                MapData map;
                if (playerData.getSlotState(0) != SlotState.FILLED) {
                    map = hub.handler().createMapForPlayerInSlot(playerData, 0);
                } else {
                    map = hub.mapService().getMap(playerData.id(), playerData.getMapSlot(0));
                }

                hub.handler().editMap(player, map.id());

                try {
                    Thread.sleep(500);
                } catch (Exception ignored) {
                }
                MinecraftServer.getCommandManager().execute(player, "tool create wand");
                MinecraftServer.getCommandManager().execute(player, "sel type line");
            }
        });

//        var tube = new Entity(EntityType.ITEM_DISPLAY) {{
//            hasPhysics = false;
//        }};
//        tube.setNoGravity(true);
//        var tubeMeta = (ItemDisplayMeta) tube.getEntityMeta();
//        tubeMeta.setItemStack(ItemStack.builder(Material.STICK).meta(b -> b.customModelData(1004)).build());
//        tubeMeta.setScale(new Vec(4, 4, 4));
//        tubeMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
//        //todo set width and height of model
//        tube.setInstance(player.getInstance(), player.getPosition().sub(0, 31, 0)).join();
//
//        var arm = new Entity(EntityType.ITEM_DISPLAY) {{
//            hasPhysics = false;
//        }};
//        arm.setNoGravity(true);
//        var armMeta = (ItemDisplayMeta) arm.getEntityMeta();
//        armMeta.setItemStack(ItemStack.builder(Material.STICK).meta(b -> b.customModelData(1005)).build());
//        armMeta.setScale(new Vec(4, 4, 4));
//        armMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
//        //todo set width and height of model
//        arm.setInstance(player.getInstance(), player.getPosition().sub(0, 31, 0)).join();
//        player.getInstance().setBlock(player.getPosition().sub(0, 31, 0), Block.TNT);
//
//        var pos = new AtomicDouble(0);
//        MinecraftServer.getSchedulerManager()
//                .buildTask(() -> {
//                    tubeMeta.setNotifyAboutChanges(false);
//                    armMeta.setNotifyAboutChanges(false);
//
//                    tubeMeta.setInterpolationDuration(80);
//                    armMeta.setInterpolationDuration(80);
//
//                    tubeMeta.setInterpolationStartDelta(1);
//                    armMeta.setInterpolationStartDelta(1);
//
//                    var newRot = pos.addAndGet(180) % 360;
//                    var rot = new Quaternion(new Vec(0, 1, 0), Math.toRadians(newRot));
//                    tubeMeta.setLeftRotation(rot.into());
//                    armMeta.setRightRotation(rot.into());
//
//                    if (newRot == 180) {
//                        armMeta.setTranslation(new Vec(0, 2, 0));
//                    } else {
//                        armMeta.setTranslation(new Vec(0, 0, 0));
//                    }
//
//                    tubeMeta.setNotifyAboutChanges(true);
//                    armMeta.setNotifyAboutChanges(true);
//                })
//                .delay(5, net.minestom.server.utils.time.TimeUnit.SECOND)
//                .repeat(5, net.minestom.server.utils.time.TimeUnit.SECOND)
//                .schedule();

    }

}
