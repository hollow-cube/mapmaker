package net.hollowcube.mapmaker.dev;

import io.helidon.health.HealthSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.GroupSection;
import net.hollowcube.map.MapServer;
import net.hollowcube.mapmaker.ServerRuntime;
import net.hollowcube.mapmaker.facet.Facet;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.HubServerImpl;
import net.hollowcube.mapmaker.hub.gui.map.MapSlotsView;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.hollowcube.terraform.compat.worldedit.TerraformWorldEdit;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.storage.FileStorageS3;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class DevServer {
    public static void main(String[] args) {
        System.setProperty("minestom.terminal.disabled", "true");
        System.setProperty("hc.instance.temp_dir", "./bin/development/build/local/local-maps");

        var minecraftServer = MinecraftServer.init();
        var server = new DevServer();

        WebServer webServer = WebServer.builder()
                .port(9124)
                .addRouting(Routing.builder()
                        .register(HealthSupport.builder()
                                .webContext("alive")
                                .addLiveness(() -> HealthCheckResponse.up("mapmaker"))
                                .build())
                        .register(HealthSupport.builder()
                                .webContext("ready")
                                .addReadiness(server.readinessChecks())
                                .build())
                        .build())
                .build();
        webServer.start()
                .thenAccept(ws -> System.out.println("Web server is running at :" + ws.port()));

        server.start();
        minecraftServer.start("0.0.0.0", 25565);

        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
                webServer.shutdown();
                ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });
    }

    private PlayerStorage playerStorage;
    private MapStorage mapStorage;
    private SaveStateStorage saveStateStorage;

    private HubServer hub;
    private MapServer maps;

    public DevServer() {

    }

    public void start() {
        MojangAuth.init();

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProvider.get2(component);

        var mongoUri = System.getenv("MM_MONGO_URI");
        if (mongoUri == null) {
            this.playerStorage = PlayerStorage.memory();
            this.mapStorage = MapStorage.memory();
            this.saveStateStorage = SaveStateStorage.memory();
        } else {
            this.playerStorage = PlayerStorage.mongo(mongoUri);
            this.mapStorage = MapStorage.mongo(mongoUri);
            this.saveStateStorage = SaveStateStorage.mongo(mongoUri);
        }

        StaticAbuse.mapStorage = mapStorage;

        var s3Address = System.getenv("MM_S3_ADDRESS");
        if (s3Address == null) s3Address = "http://localhost:9000/";
        var s3AccessKey = System.getenv("MM_S3_ACCESS_KEY");
        var s3SecretKey = System.getenv("MM_S3_SECRET_KEY");
        var worldManager = new WorldManager(FileStorageS3.connect(s3Address, s3AccessKey, s3SecretKey));

        this.maps = new MapServer(saveStateStorage);
        this.hub = new HubServerImpl(playerStorage, mapStorage, worldManager, maps);

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
        eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
        eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);

        int i = 0;
        for (var facet : ServiceLoader.load(Facet.class)) {
            facet.hook(MinecraftServer.process());
            i++;
        }
        System.out.println("loaded " + i + " facets");

        var cmd = new Command("test");
        cmd.setDefaultExecutor((sender, context) -> {
            var player = (Player) sender;
            var sec = new GroupSection(9, 3);
            sec.add(0, 0, new MapSlotsView(player.getTag(PlayerData.DATA)));
            new RouterSection(sec).showToPlayer(player);
        });
        MinecraftServer.getCommandManager().register(cmd);

        TerraformWorldEdit.init();
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
                        data.setUnlockedMapSlots(3);
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
        player.setPermissionLevel(4);

        //todo temp. PLAYER_ID is the players network ID (not necessarily their uuid, for bedrock or other users)
        player.setTag(PlayerData.PLAYER_ID, player.getUuid().toString());

        // Alpha watermark
        var runtime = ServerRuntime.getRuntime();
        String watermarkString = String.format("MapMaker %s (%s), Not representative of final product", runtime.version(), runtime.commit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString)
                .color(TextColor.color(78, 92, 36)), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
    }

}
