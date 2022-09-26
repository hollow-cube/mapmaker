package net.hollowcube.server.events.listeners;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoginEvent;

public class ListenerManager {
    private static GlobalEventHandler globalEventHandler;

    public ListenerManager() {
        globalEventHandler = MinecraftServer.getGlobalEventHandler();
    }

    public void registerListeners() {
        globalEventHandler.addListener(PlayerLoginEvent.class, PlayerJoinServerListener::onPlayerJoinServer);
    }
}
