package net.hollowcube.command.demo;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandManager2Impl;
import net.hollowcube.command.util.CommandHandlingPlayer2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

public class DemoServer {
    public static void main(String[] args) {
        var server = MinecraftServer.init();

        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 39, Block.STONE));

        var commandManager = new CommandManager2Impl();
        commandManager.register("test", new CommandBuilder()
                .executes((sender, context) -> sender.sendMessage("test no arg syntax"))
                .node());

        MinecraftServer.getConnectionManager().setPlayerProvider(CommandHandlingPlayer2.createDefaultProvider(commandManager));

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });

        server.start("0.0.0.0", 25565);
    }
}
