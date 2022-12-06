package net.hollowcube.mapmaker.dev;

import io.helidon.health.HealthSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.facet.Facet;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.handler.MapHandlerImpl;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.mapmaker.storage.Storage;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.hollowcube.terraform.compat.worldedit.TerraformWorldEdit;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                                .addLiveness()
                                .build())
                        .register(HealthSupport.builder()
                                .webContext("ready")
                                .addReadiness()
                                .build())
                        .build())
                .build();
        webServer.start()
                .thenAccept(ws -> System.out.println("Web server is running at :" + ws.port()));
        //todo handle kill code and stop everything gracefully.

        server.start();
        minecraftServer.start("0.0.0.0", 25565);
    }

    private PlayerStorage playerStorage;
    private MapStorage mapStorage;

    private HubServer hub;
    private MapServer maps;

    public DevServer() {

    }

    public void start() {
        MojangAuth.init();

        var mongoUri = System.getenv("MM_MONGO_URI");
        if (mongoUri == null) {
            this.playerStorage = PlayerStorage.memory();
            this.mapStorage = MapStorage.memory();
        } else {
            this.playerStorage = PlayerStorage.mongo(mongoUri);
            this.mapStorage = MapStorage.mongo(mongoUri);
        }

        StaticAbuse.mapStorage = mapStorage;

        this.hub = new HubServer();
        this.maps = new MapServer();

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
        eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
        eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);

        registerCommands();

        int i = 0;
        for (var facet : ServiceLoader.load(Facet.class)) {
            facet.hook(MinecraftServer.process());
            i++;
        }
        System.out.println("loaded " + i + " facets");

        TerraformWorldEdit.init();
    }

    public @NotNull List<HealthCheck> readinessChecks() {
        return List.of(
                () -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"),
                () -> HealthCheckResponse.up("mapmaker")
        );
    }

    private void registerCommands() {
        var commands = MinecraftServer.getCommandManager();
        commands.register(new MapCommand(new MapHandlerImpl(mapStorage, maps))); //todo move me to map server
    }

    private void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        var player = event.getPlayer();
        playerStorage.getPlayerByUuid(event.getPlayerUuid().toString())
                .exceptionallyCompose(e -> {
                    if (Storage.isNotFound(e)) {
                        var data = new PlayerData();
                        data.setId(event.getPlayerUuid().toString());
                        data.setUuid(event.getPlayerUuid().toString());
                        return playerStorage.createPlayer(data);
                    }
                    return CompletableFuture.failedFuture(e);
                })
                .thenAccept(data -> player.setTag(PlayerData.DATA, data))
                .exceptionally(e -> {
                    System.out.println("Failed to load player data for " + player.getUsername());
                    e.printStackTrace();
                    player.kick(Component.text("Failed to load data"));
                    return null;
                });
    }

    private void handleLogin(PlayerLoginEvent event) {
        event.setSpawningInstance(hub.getInstance());
        event.getPlayer().setRespawnPoint(hub.getSpawnPos());
    }

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        //todo this should be handled by hub
        if (event.getSpawnInstance().hasTag(MapWorld.MAP_ID))
            return;

        var player = event.getPlayer();
        player.setPermissionLevel(4);
        player.setGameMode(GameMode.CREATIVE);

        //todo temp
        player.setTag(PlayerData.PLAYER_ID, player.getUuid().toString());

        // Alpha watermark
        String watermarkString = String.format("MapMaker %s (%s), Not representative of final product", Constants.VERSION, Constants.COMMIT_HASH);
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString)
                .color(TextColor.color(78, 92, 36)), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
    }

}
