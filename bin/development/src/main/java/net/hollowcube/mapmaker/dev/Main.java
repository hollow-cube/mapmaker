package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.gui.inventory.InventoryUtils;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.handler.MapHandlerImpl;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.Block;

public class Main {
    public static void main(String[] args) {
        var server = MinecraftServer.init();

        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });
        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();
            player.setPermissionLevel(4);
            player.setGameMode(GameMode.CREATIVE);

            //todo temp
            player.setTag(PlayerData.PLAYER_ID, player.getUuid().toString());

            player.sendMessage(
                    Component.text("Welcome to ", NamedTextColor.WHITE)
                            .append(Component.text("Map Maker!", NamedTextColor.AQUA)));
            InventoryUtils.setPlayerLobbyInventory(player);
        });

        var mapHandler = new MapHandlerImpl(MapStorage.memory(), new LocalMapOrchestrator());

        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new MapCommand(mapHandler));

        server.start("0.0.0.0", 25565);
    }
}
