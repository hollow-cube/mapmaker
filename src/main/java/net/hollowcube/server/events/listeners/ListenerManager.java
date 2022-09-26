package net.hollowcube.server.events.listeners;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;

import static omega.mapmaker.events.listeners.PlayerJoinServerListener.onPlayerJoinServer;

public class ListenerManager {
    private static GlobalEventHandler globalEventHandler;

    public ListenerManager() {
        globalEventHandler = MinecraftServer.getGlobalEventHandler();
    }

    public void registerListeners() {
        onPlayerJoinServer(globalEventHandler);
    }

    public GlobalEventHandler getGlobalEventHandler() {
        return globalEventHandler;
    }
}
