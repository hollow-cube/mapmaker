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
import net.hollowcube.mapmaker.dev.command.DebugCommand;
import net.hollowcube.mapmaker.dev.command.ToggleScoreboardCommand;
import net.hollowcube.mapmaker.dev.config.Config;
import net.hollowcube.mapmaker.dev.config.NewConfigProvider;
import net.hollowcube.mapmaker.dev.http.HttpConfig;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
import net.hollowcube.mapmaker.model.DisplayNameBuilder;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.permission.MapPermissionManager;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.service.PlayerServiceImpl;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.mapmaker.ui.Scoreboards;
import net.hollowcube.mapmaker.storage.WhitelistStorage;
import net.hollowcube.mapmaker.storage.*;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.storage.FileStorageMemory;
import net.hollowcube.world.storage.FileStorageS3;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
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
    private WhitelistStorage whitelistStorage;

    private WorldManager worldManager;

    private PlatformPermissionManager platformPermissions;
    private MapPermissionManager mapPermissions;

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

            if (System.getenv("MM_WHITELIST_DEV") != null) {
                this.whitelistStorage = WhitelistStorage.memory();
            } else {
                scope.fork(() -> {
                    this.whitelistStorage = WhitelistStorage.mongo(config.mongo());
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
            } else {
                this.worldManager = new WorldManager(FileStorageS3.connect(config.s3().uri()));
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


        // Start phase 2
        // Start hub and map server and bridge them.

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var bridge = new DevServerBridge();

            this.hub = new DevHubServer(bridge, mapStorage, playerStorage, metricStorage, worldManager, platformPermissions, mapPermissions);
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
            DisplayNameBuilder.init(playerStorage);

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

        // Whitelist check
        boolean whitelisted = whitelistStorage.isWhitelisted(playerId);
        if (!whitelisted) {
            player.kick(Component.text("You are not whitelisted on this server!", NamedTextColor.RED));
            return;
        }

        PlayerData playerData;
        try {
            playerData = playerStorage.getPlayerByUuid(playerId);
        } catch (PlayerStorage.NotFoundError e) {
            var data = new PlayerData();
            data.setId(event.getPlayerUuid().toString());
            data.setUuid(event.getPlayerUuid().toString());
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

    //FluentFuture.from(playerStorage.getPlayerByUuid(event.getPlayerUuid().toString()))
    //                    .catchingAsync(PlayerStorage.NotFoundError.class, err -> {
    //                        var data = new PlayerData();
    //                        data.setId(event.getPlayerUuid().toString());
    //                        data.setUuid(event.getPlayerUuid().toString());
    //                        data.setDisplayName(
    //                                DisplayNameBuilder.playerToDisplayName(event.getPlayer(), this.platformPermissions));
    //                        data.setUnlockedMapSlots(PlayerData.DEFAULT_UNLOCKED_MAP_SLOTS);
    //                        MetricsHelper.get().recordMetricFirstJoinTime(event.getPlayerUuid().toString());
    //                        return playerStorage.createPlayer(data);
    //                    }, Runnable::run)
    //                    .transform(data -> {
    //                        player.setTag(PlayerData.PLAYER_ID, data.getId());
    //                        player.setTag(PlayerData.DATA, data);
    //
    //                        boolean changed = false;
    //
    //                        // Update their display name (move to be callback whenever their perms change)
    //                        var newDisplayName = DisplayNameBuilder.playerToDisplayName(player, platformPermissions);
    //                        if (data.getDisplayName() != newDisplayName) {
    //                            changed = true;
    //                            data.setDisplayName(newDisplayName);
    //                        }
    //
    //                        // todo this cleanup step should probably be moved
    //                        // Cleanup maps which are actually gone
    //                        for (int i = 0; i < data.getUnlockedMapSlots(); i++) {
    //                            var mapId = data.getMapSlot(i);
    //                            if (mapId != null) {
    //                                try {
    //                                    var map = mapStorage.getMapById(mapId).get();
    //                                    if (map.isPublished()) {
    //                                        // Map is published, delete from slots.
    //                                        changed = true;
    //                                        data.setMapSlot(i, null);
    //                                        logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was published.", mapId, data.getId());
    //                                    }
    //                                } catch (Exception e) {
    //                                    if (e instanceof InterruptedException)
    //                                        Thread.currentThread().interrupt();
    //                                    if (e instanceof MapStorage.NotFoundError) {
    //                                        // Map is gone, delete from slots
    //                                        changed = true;
    //                                        data.setMapSlot(i, null);
    //                                        logger.log(System.Logger.Level.INFO, "Removed map {0} from player {1} because it was deleted.", mapId, data.getId());
    //                                    }
    //
    //                                    logger.log(System.Logger.Level.ERROR, "Failed to load map data for " + event.getUsername(), e);
    //                                    player.kick(Component.text("Failed to load map data! Please try again later."));
    //                                    return null;
    //                                }
    //                            }
    //                        }
    //                        if (changed) {
    //                            playerStorage.updatePlayer(data).toCompletableFuture().join();
    //                        }
    //
    //                        return null;
    //                    }, ForkJoinPool.commonPool())
    //                    .get();
    //        } catch (Exception e) {
    //            if (e instanceof InterruptedException)
    //                Thread.currentThread().interrupt();
    //
    //            logger.log(System.Logger.Level.ERROR, "Failed to load player data for " + event.getUsername(), e);
    //            player.kick(Component.text("Failed to load player data! Please try again later."));
    //        }
    //    }

    private void handleLogin(PlayerLoginEvent event) {
        event.setSpawningInstance(hub.world().instance());
        event.getPlayer().setRespawnPoint(new Pos(0.5, 40, 0.5));
        //TODO whitelist logic
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
        String name = DisplayNameBuilder.playerToDisplayName(player, platformPermissions);
        player.sendMessage(Component.text("Hello, ").append(Component.text(name)));
        player.setPermissionLevel(4);

        // Alpha watermark
        var runtime = ServerRuntime.getRuntime();
        String watermarkString = String.format("MapMaker %s+%s, Not representative of final product", runtime.version(), runtime.commit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString).color(TextColor.color(78, 92, 36)), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

        Scoreboards.showPlayerLobbyScoreboard(player);
        Scoreboards.setScoreboardVisibility(player, Boolean.TRUE);
    }
}
