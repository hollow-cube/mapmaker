package net.hollowcube.server;

import net.hollowcube.server.commands.CommandLoader;
import net.hollowcube.server.events.listeners.ListenerManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.MojangAuth;

import static net.hollowcube.server.events.once.ServerStartupEvent.onServerStartup;

public class StartServer {
    public static void main(String[] args) {
        // Initialization
        MinecraftServer minecraftServer = MinecraftServer.init();
        MojangAuth.init();
        MapMaker instance = new MapMaker();

        // Register all commands
        CommandLoader commands = new CommandLoader();
        commands.registerCommands();

        // Register all listeners
        ListenerManager listeners = new ListenerManager();
        listeners.registerListeners();

        // Run basic server startup
        onServerStartup();

        minecraftServer.start("0.0.0.0", 25565);
    }
}
