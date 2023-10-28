package net.hollowcube.command.example.server;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.HelpCommand;
import net.hollowcube.command.example.FlipCommand;
import net.hollowcube.command.example.ParentCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;

public class DemoServer {
    public static void main(String[] args) {
        var server = MinecraftServer.init();

        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 39, Block.STONE));

        var commandManager = new CommandManager();
        commandManager.register(new HelpCommand(commandManager));
        commandManager.register(new FlipCommand(null, null));
        commandManager.register(new ParentCommand());

        var packetListener = MinecraftServer.getPacketListenerManager();
        packetListener.setListener(ClientCommandChatPacket.class,
                (packet, player) -> commandManager.execute(player, packet.message()));
        packetListener.setListener(ClientTabCompletePacket.class, (packet, player) -> {
            var result = commandManager.suggestions(player, packet.text().substring(1));
            if (!result.isEmpty()) {
                var response = new TabCompletePacket(
                        packet.transactionId(),
                        result.getStart() + 1, // add one because of the '/' sent by the client
                        result.getLength(),
                        result.getEntries().stream()
                                .map(s -> new TabCompletePacket.Match(s.replacement(), s.tooltip()))
                                .toList()
                );
                player.sendPacket(response);
            }
        });

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });
        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();

            player.sendPacket(commandManager.commandPacket(player));
        });

        server.start("0.0.0.0", 25565);
    }
}
