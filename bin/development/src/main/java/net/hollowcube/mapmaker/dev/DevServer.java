package net.hollowcube.mapmaker.dev;

import com.google.common.util.concurrent.AtomicDouble;
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
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.dev.command.DebugCommand;
import net.hollowcube.mapmaker.dev.command.FakePlayerCommand;
import net.hollowcube.mapmaker.dev.command.ToggleScoreboardCommand;
import net.hollowcube.mapmaker.dev.config.Config;
import net.hollowcube.mapmaker.dev.config.NewConfigProvider;
import net.hollowcube.mapmaker.dev.http.HttpConfig;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.hub.legacy.LegacyMapService;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.permission.MapPermissionManager;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.service.PlayerService;
import net.hollowcube.mapmaker.service.PlayerServiceImpl;
import net.hollowcube.mapmaker.storage.*;
import net.hollowcube.mapmaker.ui.Scoreboards;
import net.hollowcube.mapmaker.ui.TabLists;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.storage.FileStorageMemory;
import net.hollowcube.world.storage.FileStorageS3;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
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

        System.setProperty("minestom.terminal.disabled", "true");
        System.setProperty("minestom.async-commands", "true");
        System.setProperty("minestom.event.multiple-parents", "true");
        System.setProperty("hc.instance.temp_dir", "./bin/development/build/local/local-maps");
        System.setProperty("minestom.chunk-view-distance", "16");

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
        var config = Config.loadFromFile(Path.of("config.yaml"));
        var configProvider = NewConfigProvider.loadFromFile(Path.of("config.yaml"));

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

    private PlayerStorage playerStorage;
    private MapStorage mapStorage;
    private SaveStateStorage saveStateStorage;
    private MetricStorage metricStorage;

    private WorldManager worldManager;

    private PlatformPermissionManager platformPermissions;
    private MapPermissionManager mapPermissions;

    private PlayerService playerService;
    private LegacyMapService legacyMapService = null;

    private DevHubServer hub;
    private DevMapServer maps;

    public DevServer() {

    }

    @Blocking
    public void start(@NotNull Config config, @NotNull NewConfigProvider configProvider) {
        MojangAuth.init();

        // Start phase 1
        // Connect to low level services

        var mongoConfig = config.mongo();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            if (System.getenv("MM_PLAYER_STORAGE_DEV") != null) {
                this.playerStorage = PlayerStorage.memory();
            } else {
                scope.fork(() -> {
                    this.playerStorage = PlayerStorage.mongo(mongoConfig);
                    return null;
                });
            }

            if (System.getenv("MM_MAP_STORAGE_DEV") != null) {
                this.mapStorage = MapStorage.memory();
            } else {
                scope.fork(() -> {
                    this.mapStorage = MapStorage.mongo(mongoConfig);
                    return null;
                });
            }

            if (System.getenv("MM_SAVESTATE_DEV") != null) {
                this.saveStateStorage = SaveStateStorage.memory();
            } else {
                scope.fork(() -> {
                    this.saveStateStorage = SaveStateStorage.mongo(mongoConfig);
                    return null;
                });
            }


            if (System.getenv("MM_METRICS_STORAGE_DEV") != null) {
                this.metricStorage = MetricStorage.memory();
                MetricsHelper.init(metricStorage);
            } else {
                scope.fork(() -> {
                    this.metricStorage = MetricStorage.mongo(config.mongo());
                    MetricsHelper.init(metricStorage);
                    return null;
                });
            }

            if (System.getenv("MM_WORLD_MANAGER_DEV") != null) {
                this.worldManager = new WorldManager(new FileStorageMemory());
                this.legacyMapService = LegacyMapService.create("s3://231751fdba5d68aa03ae55c2d817443a:34430f6c5385fe383d52ed8ac420a7173a76045078608f128772efc9ed160f02@bdb9cb2904188b760591dc1589a1ccf3.r2.cloudflarestorage.com/mapmaker");
            } else {
                scope.fork(() -> {
                    try {
                        this.worldManager = new WorldManager(FileStorageS3.connect(config.s3().uri()));
                        this.legacyMapService = LegacyMapService.create(config.s3().uri());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            // SpiceDB
            if (System.getenv("MM_MAP_PERMISSIONS_DEV") != null) {
                this.platformPermissions = PlatformPermissionManager.noop();
                this.mapPermissions = MapPermissionManager.noop();
            } else {
                scope.fork(() -> {
                    this.platformPermissions = PlatformPermissionManager.spicedb(config.spicedb());
                    return null;
                });
                scope.fork(() -> {
                    this.mapPermissions = MapPermissionManager.spicedb(config.spicedb());
                    return null;
                });
            }

            scope.join();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
            System.exit(1);
        }

        playerService = new PlayerServiceImpl(playerStorage, platformPermissions);

        // Start phase 2
        // Start hub and map server and bridge them.

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var bridge = new DevServerBridge();

            this.hub = new DevHubServer(bridge, mapStorage, playerStorage, metricStorage, worldManager, platformPermissions, mapPermissions, playerService, legacyMapService);
            this.maps = new DevMapServer(bridge, mapStorage, metricStorage, saveStateStorage, worldManager, platformPermissions);
            bridge.setHubServer(hub);
            bridge.setMapServer(maps);

            scope.fork(Executors.callable(this.hub::init));
            scope.fork(Executors.callable(() -> this.maps.init(configProvider)));

            scope.join();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
            System.exit(1);
        }


        // Start phase 3
        // Load all facets & other misc startup tasks like setting up some events & minestom properties


        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            MinecraftServer.getCommandManager().register(new DebugCommand(playerStorage, mapStorage));
            MinecraftServer.getCommandManager().register(new ToggleScoreboardCommand());
            MinecraftServer.getCommandManager().register(new FakePlayerCommand());

            var eventHandler = MinecraftServer.getGlobalEventHandler();
            eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
            eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
            eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);
            eventHandler.addListener(PlayerDisconnectEvent.class, this::handleDisconnect);
            eventHandler.addListener(PlayerChatEvent.class, event -> event.setChatFormat(e -> {
                var player = event.getPlayer();
                var username = player.getUsername();
                return Component.translatable("chat.type.text").args(Component.text(username), Component.text(event.getMessage().replace(":skull:", "\uEff5"), NamedTextColor.RED));
            }));
            eventHandler.addListener(MapDeletedEvent.class, event -> {
                for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                    var playerData = PlayerData.fromPlayer(player);
                    for (int i = 0; i < playerData.getUnlockedMapSlots(); i++) {
                        var map = playerData.getMapSlot(i);
                        if (map != null && map.equals(event.mapId())) {
                            logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was deleted.", event.mapId(), playerData.getId());
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

            Scoreboards.init();
            TabLists.init();

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
        var playerId = event.getPlayerUuid().toString();

        PlayerData playerData;
        try {
            playerData = playerStorage.getPlayerByUuid(playerId);

            // We know the player exists so this call is safe
            var displayName = playerService.getDisplayName(playerId);
            playerData.setDisplayName(displayName);
        } catch (PlayerStorage.NotFoundError e) {
            var data = new PlayerData();
            data.setId(event.getPlayerUuid().toString());
            data.setUuid(event.getPlayerUuid().toString());
            data.setUsername(event.getUsername());
            data.setDisplayName(Component.text(event.getUsername()));
            data.setUnlockedMapSlots(PlayerData.DEFAULT_UNLOCKED_MAP_SLOTS);
            MetricsHelper.get().recordMetricFirstJoinTime(event.getPlayerUuid().toString());
            playerData = playerStorage.createPlayer(data);
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to load player data for " + event.getUsername(), e);
            player.kick(Component.text("Failed to load player data! Please try again later."));
            return;
        }

        player.setTag(PlayerData.PLAYER_ID, playerData.getId());
        player.setTag(PlayerData.DATA, playerData);

        // todo this cleanup step should probably be moved
        // Cleanup maps which are actually gone
        boolean changed = false;
        for (int i = 0; i < playerData.getUnlockedMapSlots(); i++) {
            var mapId = playerData.getMapSlot(i);
            if (mapId != null) {
                try {
                    var map = mapStorage.getMapById(mapId);
                    if (map.isPublished()) {
                        // Map is published, delete from slots.
                        changed = true;
                        playerData.setMapSlot(i, null);
                        logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was published.", mapId, playerData.getId());
                    }
                } catch (MapStorage.NotFoundError e) {
                    // Map is gone, delete from slots
                    changed = true;
                    playerData.setMapSlot(i, null);
                    logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was deleted.", mapId, playerData.getId());
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Failed to load map data for " + event.getUsername(), e);
                    player.kick(Component.text("Failed to load map data! Please try again later."));
                    return;
                }
            }
        }
        if (changed) {
            playerStorage.updatePlayer(playerData);
        }
    }

    private void handleLogin(PlayerLoginEvent event) {
        event.setSpawningInstance(hub.world().instance());
        event.getPlayer().setRespawnPoint(new Pos(0.5, 4, 0.5));
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        Thread.startVirtualThread(() -> {
            var player = event.getPlayer();
            var playerData = PlayerData.fromPlayer(player);

            try {
                //todo we may want a dead letter or something, but im not sure where to put it. This requires a lot more thought
                playerStorage.updatePlayer(playerData);

                //TODO handle on proxy, along with other relevant methods to make more accurate as well (rounding isn't accurate)
                //todo also timing shouldnt be done using alive ticks. They may be killed for whatever reason at some point.
                MetricsHelper.get().recordMetricSessionPlayTimeMs(player.getUuid().toString(), player.getAliveTicks() * 50);

                logger.log(System.Logger.Level.INFO, "Saved player data for {0}", playerData.getId());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to save player data for " + playerData.getId(), e);
            }
        });
    }

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        //todo this gamemode/fly/permission level stuff should be handled by the hub server
        var player = event.getPlayer();
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlying(true);
        player.setPermissionLevel(4);

        // Alpha watermark
        var runtime = ServerRuntime.getRuntime();
        String watermarkString = String.format("MapMaker %s+%s, Not representative of final product", runtime.version(), runtime.commit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString).color(TextColor.color(78, 92, 36)), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

        Scoreboards.showPlayerLobbyScoreboard(player);
        Scoreboards.setScoreboardVisibility(player, Boolean.TRUE);
        TabLists.showPlayerGlobalTabList(player);


//        Thread.startVirtualThread(() -> {
//            if (System.getenv("MAPMAKER_MAP_DEV") != null) {
//
//                MapData map;
//                var playerData = PlayerData.fromPlayer(player);
//                if (playerData.getSlotState(0) != PlayerData.SLOT_STATE_IN_USE) {
//                    map = new MapData();
//                    map.setOwner(playerData.getId());
//                    map = hub.handler().createMapForPlayerInSlot(playerData, map, 0);
//                } else {
//                    map = hub.mapStorage().getMapById(playerData.getMapSlot(0));
//                }
//
//                hub.handler().editMap(player, map.getId());
//
//                try {
//                    Thread.sleep(500);
//                } catch (Exception ignored) {}
//                MinecraftServer.getCommandManager().execute(player, "give mapmaker:path_tool");
//            }
//        });


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
