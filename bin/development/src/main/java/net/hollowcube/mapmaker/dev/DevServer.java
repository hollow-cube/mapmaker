package net.hollowcube.mapmaker.dev;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.helidon.health.HealthSupport;
import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.dev.config.Config;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.permission.MapPermissionManager;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.service.PlayerServiceImpl;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
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
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
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

        // Prometheus JVM exporters
        DefaultExports.initialize();

        // Load config
        var config = Config.loadFromFile(Path.of("config.yaml"));

        // Begin server initialization
        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getExceptionManager().setExceptionHandler(t ->
                logger.log(System.Logger.Level.ERROR, "An uncaught exception has been handled", t));
        var server = new DevServer();

        // Add health check & metrics web server.
        WebServer webServer = WebServer.builder()
                .host(config.http().host())
                .port(config.http().port())
                .addRouting(Routing.builder()
                        .register(HealthSupport.builder()
                                .webContext("alive")
                                .addLiveness(() -> HealthCheckResponse.up("mapmaker"))
                                .build())
                        .register(HealthSupport.builder()
                                .webContext("ready")
                                .addReadiness(server.readinessChecks())
                                .build())
                        .register(PrometheusSupport.create())
                        .build())
                .build();
        webServer.start().thenAccept(ws -> logger.log(System.Logger.Level.INFO,
                "Web server is running at " + config.http().host() + ":" + ws.port()));

        // Finish server initialization
        server.start(config);
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

    private PlatformPermissionManager platformPermissions;
    private MapPermissionManager mapPermissions;

    private DevHubServer hub;
    private DevMapServer maps;

    public DevServer() {

    }

    public void start(@NotNull Config config) {
        MojangAuth.init();

        var startupTasks = new ArrayList<ListenableFuture<Void>>();

        // Start phase 1
        // Connect to low level services

        if (System.getenv("MM_PLAYER_STORAGE_DEV") != null) {
            this.playerStorage = PlayerStorage.memory();
        } else {
            startupTasks.add(Futures.transform(
                    PlayerStorage.mongo(config.mongo()),
                    playerStorage -> {
                        this.playerStorage = playerStorage;
                        return null;
                    },
                    Runnable::run
            ));
        }

        if (System.getenv("MM_MAP_STORAGE_DEV") != null) {
            this.mapStorage = MapStorage.memory();
        } else {
            startupTasks.add(Futures.transform(
                    MapStorage.mongo(config.mongo()),
                    mapStorage -> {
                        this.mapStorage = mapStorage;
                        return null;
                    },
                    Runnable::run
            ));
        }

        if (System.getenv("MM_SAVESTATE_DEV") != null) {
            this.saveStateStorage = SaveStateStorage.memory();
        } else {
            startupTasks.add(Futures.transform(
                    SaveStateStorage.mongo(config.mongo()),
                    saveStateStorage -> {
                        this.saveStateStorage = saveStateStorage;
                        return null;
                    },
                    Runnable::run
            ));
        }

        WorldManager worldManager;
        if (System.getenv("MM_WORLD_MANAGER_DEV") != null) {
            worldManager = new WorldManager(new FileStorageMemory());
        } else {
            worldManager = new WorldManager(FileStorageS3.connect(config.s3().uri()));
        }

        // SpiceDB
        if (System.getenv("MM_MAP_PERMISSIONS_DEV") != null) {
            this.platformPermissions = PlatformPermissionManager.noop();
            this.mapPermissions = MapPermissionManager.noop();
        } else {
            startupTasks.add(Futures.transform(
                    PlatformPermissionManager.spicedb(config.spicedb()),
                    platformPermissions -> {
                        this.platformPermissions = platformPermissions;
                        return null;
                    },
                    Runnable::run
            ));
            startupTasks.add(Futures.transform(
                    MapPermissionManager.spicedb(config.spicedb()),
                    mapPermissions -> {
                        this.mapPermissions = mapPermissions;
                        return null;
                    },
                    Runnable::run
            ));
        }

        try {
            Futures.whenAllComplete(startupTasks).call(() -> null, Runnable::run).get();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
            System.exit(1);
        }


        // Start phase 2
        // Start hub and map server and bridge them.

        startupTasks.clear();

        var bridge = new DevServerBridge();

        this.hub = new DevHubServer(bridge, mapStorage, playerStorage, worldManager, platformPermissions, mapPermissions);
        this.maps = new DevMapServer(bridge, mapStorage, saveStateStorage, worldManager, platformPermissions);
        bridge.setHubServer(hub);
        bridge.setMapServer(maps);
        startupTasks.add(this.hub.init());
        startupTasks.add(this.maps.init());

        try {
            Futures.whenAllComplete(startupTasks).call(() -> null, Runnable::run).get();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
            System.exit(1);
        }


        // Start phase 3
        // Load all facets & other misc startup tasks like setting up some events & minestom properties
        startupTasks.clear();

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
        eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
        eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);
        eventHandler.addListener(PlayerChatEvent.class, event -> {
            event.setChatFormat(e -> {
                var player = event.getPlayer();
                var username = player.getUsername();
                return Component.translatable("chat.type.text")
                        .args(Component.text(username), Component.text(event.getMessage().replace(":skull:", "\uEff5"), NamedTextColor.RED));
            });
        });

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProvider.get2(component);

        int i = 0;
        for (var facet : ServiceLoader.load(Facet.class)) {
            System.out.println(facet);
            startupTasks.add(facet.hook(MinecraftServer.process()));
            i++;
        }
        logger.log(System.Logger.Level.INFO, "Loaded {0} facets.", i);

        try {
            Futures.whenAllComplete(startupTasks).call(() -> null, Runnable::run).get();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed during startup", e);
            System.exit(1);
        }
    }

    public @NotNull List<HealthCheck> readinessChecks() {
        return List.of(
                () -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"),
                () -> HealthCheckResponse.up("mapmaker")
        );
    }

    private void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        var player = event.getPlayer();
        playerStorage.getPlayerByUuid(event.getPlayerUuid().toString())
                .flatMapErr(err -> {
                    if (err.is(PlayerStorage.ERR_NOT_FOUND)) {
                        var data = new PlayerData();
                        data.setId(event.getPlayerUuid().toString());
                        data.setUuid(event.getPlayerUuid().toString());
                        data.setUnlockedMapSlots(PlayerData.DEFAULT_UNLOCKED_MAP_SLOTS);
                        return playerStorage.createPlayer(data);
                    }
                    return FutureResult.error(err);
                })
                .then(data -> {
                    player.setTag(PlayerData.PLAYER_ID, data.getId());
                    player.setTag(PlayerData.DATA, data);
                })
                .mapErr(err -> {
                    System.out.println("Failed to load player data for " + player.getUsername() + ": " + err);
                    player.kick(Component.text("Failed to load data"));
                    return Result.ofNull();
                });
        //todo need to hold player until this finishes
    }

    private void handleLogin(PlayerLoginEvent event) {
        event.setSpawningInstance(hub.world().instance());
        event.getPlayer().setRespawnPoint(new Pos(0.5, 40, 0.5));
    }

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        var player = event.getPlayer();
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlying(true);
        player.sendMessage(Component.text("Hello, ").append(new PlayerServiceImpl().getDisplayName(player.getUuid().toString()).toCompletableFuture().join().result()));
        player.setPermissionLevel(4);

        //todo temp. PLAYER_ID is the players network ID (not necessarily their uuid, for bedrock or other users)
        player.setTag(PlayerData.PLAYER_ID, player.getUuid().toString());

        // Alpha watermark
        var runtime = ServerRuntime.getRuntime();
        String watermarkString = String.format("MapMaker %s+%s, Not representative of final product", runtime.version(), runtime.commit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString)
                .color(TextColor.color(78, 92, 36)), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

    }

}
