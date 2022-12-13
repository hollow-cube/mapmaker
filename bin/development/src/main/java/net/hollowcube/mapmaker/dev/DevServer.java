package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.handler.MapHandlerImpl;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.mapmaker.storage.Storage;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.concurrent.CompletableFuture;

public class DevServer {
    public static void main(String[] args) {
        System.setProperty("minestom.terminal.disabled", "true");
        System.setProperty("hc.instance.temp_dir", "./bin/development/build/local/local-maps");

        var server = MinecraftServer.init();

        MojangAuth.init();

        new DevServer();

        server.start("0.0.0.0", 25565);
    }

    private final PlayerStorage playerStorage;
    private final MapStorage mapStorage;

    private final HubServer hub;
    private final MapServer maps;

    public DevServer() {
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
    }

}
