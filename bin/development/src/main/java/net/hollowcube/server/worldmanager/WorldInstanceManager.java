package net.hollowcube.server.worldmanager;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;

public class WorldInstanceManager {
    private static final int MAX_PLAYERS_PER_WORLD = 50;

    private final InstanceManager instanceManager;

    private static InstanceContainer baseInstance;

    public WorldInstanceManager() {
        this.instanceManager = MinecraftServer.getInstanceManager();
        baseInstance = instanceManager.createInstanceContainer();
    }

    public InstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    public InstanceContainer getBaseInstance() {
        return baseInstance;
    }

    public static void setBaseInstance(int UUID) {

    }
}
