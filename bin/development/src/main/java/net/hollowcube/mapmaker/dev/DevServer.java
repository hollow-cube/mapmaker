package net.hollowcube.mapmaker.dev;

import com.authzed.api.v1.PermissionsServiceGrpc;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import net.hollowcube.terraform.compat.worldedit.TerraformWorldEdit;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.storage.FileStorageS3;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class DevServer {
    public static void main(String[] args) {
        long start = System.nanoTime();

        System.setProperty("minestom.terminal.disabled", "true");
        System.setProperty("hc.instance.temp_dir", "./bin/development/build/local/local-maps");

        var config = Config.loadFromFile(Path.of("config.yaml"));

        var minecraftServer = MinecraftServer.init();
        var server = new DevServer();

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
        webServer.start()
                .thenAccept(ws -> System.out.println("Web server is running at " + config.http().host() + ":" + ws.port()));

        DefaultExports.initialize();

        server.start(config);
        minecraftServer.start(config.minestom().host(), config.minestom().port());

        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
                webServer.shutdown();
                ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }

    private static final Logger logger = LoggerFactory.getLogger(DevServer.class);

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

        List<FutureResult<Void>> startupTasks = new ArrayList<>();

        // Start phase 1

        var mongoUri = System.getenv("MM_MONGO_URI");
        if (mongoUri == null) {
            this.playerStorage = PlayerStorage.memory();
            this.mapStorage = MapStorage.memory();
            this.saveStateStorage = SaveStateStorage.memory();
        } else {
            //todo all remote services should have health checks
            startupTasks.add(PlayerStorage.mongo(config.mongo())
                    .then(playerStorage -> this.playerStorage = playerStorage));
            startupTasks.add(MapStorage.mongo(config.mongo())
                    .then(mapStorage -> this.mapStorage = mapStorage));
            startupTasks.add(SaveStateStorage.mongo(config.mongo())
                    .then(saveStateStorage -> this.saveStateStorage = saveStateStorage)
                    .thenErr(err -> {
                        logger.error("Failed to connect to mongo: {}", err);
                        System.exit(1);
                    }));
        }

        var worldManager = new WorldManager(FileStorageS3.connect(config.s3().address(), config.s3().accessKey(), config.s3().secretKey()));

        // SpiceDB
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(config.spicedb().address())
                .usePlaintext()
                .build();
        BearerToken bearerToken = new BearerToken(config.spicedb().secretKey());
        PermissionsServiceGrpc.PermissionsServiceFutureStub permissionsService = PermissionsServiceGrpc.newFutureStub(channel)
                .withCallCredentials(bearerToken);
        this.platformPermissions = new PlatformPermissionManager(permissionsService);
        this.mapPermissions = new MapPermissionManager(permissionsService);

        // End phase 1
        FutureResult.allOf(startupTasks.toArray(FutureResult[]::new))
                .then(unused -> logger.info("Phase 1 complete."))
                .thenErr(err -> {
                    logger.error("failed during startup: {}", err);
                    System.exit(1);
                })
                .toCompletableFuture().join();

        // Start phase 2
        startupTasks.clear();

        var bridge = new DevServerBridge();

        this.hub = new DevHubServer(bridge, mapStorage, playerStorage, worldManager, mapPermissions);
        this.maps = new DevMapServer(bridge, mapStorage, saveStateStorage, worldManager);
        bridge.setHubServer(hub);
        bridge.setMapServer(maps);
        startupTasks.add(this.hub.init());
        startupTasks.add(this.maps.init());

        // End phase 2
        FutureResult.allOf(startupTasks.toArray(FutureResult[]::new))
                .then(unused -> logger.info("Phase 2 complete."))
                .thenErr(err -> {
                    logger.error("failed during startup: {}", err);
                    System.exit(1);
                })
                .toCompletableFuture().join();

        // Start phase 3 (other misc tasks)
        startupTasks.clear();

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
        eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
        eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProvider.get2(component);

        int i = 0;
        for (var facet : ServiceLoader.load(Facet.class)) {
            startupTasks.add(facet.hook(MinecraftServer.process()));
            i++;
        }
        logger.info("Loaded {} facets.", i);

        TerraformWorldEdit.init(); //todo only should be present in map servers

        // End phase 3
        FutureResult.allOf(startupTasks.toArray(FutureResult[]::new))
                .then(unused -> logger.info("Phase 3 complete."))
                .thenErr(err -> {
                    logger.error("failed during startup: {}", err);
                    System.exit(1);
                })
                .toCompletableFuture().join();
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
