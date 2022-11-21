package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.command.HubCommand;
import net.hollowcube.mapmaker.gui.inventory.InventoryUtils;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.handler.MapHandlerImpl;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.mapmaker.storage.Storage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        System.setProperty("minestom.terminal.disabled", "true");

        var server = MinecraftServer.init();

        MojangAuth.init();

        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = new InstanceContainer(new UUID(0, 0), DimensionType.OVERWORLD);
        instanceManager.registerInstance(instance);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setTag(HubServer.HUB_MARKER, true);

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });
        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();

            if (event.isFirstSpawn()) {
                player.setPermissionLevel(4);
                player.setGameMode(GameMode.CREATIVE);
                // Commands are first sent without an instance, so refresh when spawning
                player.refreshCommands();

                //todo temp
                player.setTag(PlayerData.PLAYER_ID, player.getUuid().toString());

                player.sendMessage(
                        Component.text("Welcome to ", NamedTextColor.WHITE)
                                .append(Component.text("Map Maker!", NamedTextColor.AQUA)));
                InventoryUtils.setPlayerLobbyInventory(player);
            }
        });
        eventHandler.addListener(PlayerChatEvent.class, event -> {
            var player = event.getPlayer();

            var data = player.getTag(PlayerData.DATA);
            System.out.println(data);
        });

        var playerStorage = PlayerStorage.memory();

        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, event -> {
            var player = event.getPlayer();
            playerStorage.getPlayerByUuid(event.getPlayerUuid().toString())
                    .exceptionallyCompose(e -> {
                        if (e == Storage.NOT_FOUND)
                            return playerStorage.createPlayer(new PlayerData(event.getPlayerUuid().toString()));
                        return CompletableFuture.failedFuture(e);
                    })
                    .thenAccept(data -> player.setTag(PlayerData.DATA, data))
                    .exceptionally(e -> {
                        System.out.println("Failed to load player data for " + player.getUsername());
                        e.printStackTrace();
                        player.kick(Component.text("Failed to load data"));
                        return null;
                    });
        });

        var mapServer = new MapServer();
        var mapHandler = new MapHandlerImpl(MapStorage.memory(), mapServer);

        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new MapCommand(mapHandler));

        server.start("0.0.0.0", 25565);
    }
}
