package net.hollowcube.server;

import net.hollowcube.server.player.MapMakerPlayerManager;
import net.hollowcube.server.worldmanager.WorldInstanceManager;

public class MapMaker {
    private static MapMaker instance;

    private final WorldInstanceManager worldInstanceManager;
    private final MapMakerPlayerManager mapMakerPlayerManager;

    public MapMaker() {
        instance = this;

        this.worldInstanceManager = new WorldInstanceManager();
        this.mapMakerPlayerManager = new MapMakerPlayerManager();
    }

    public static MapMaker getInstance() {
        return instance;
    }

    public WorldInstanceManager getWorldInstanceManager() {
        return this.worldInstanceManager;
    }

    public MapMakerPlayerManager getMapMakerPlayerManager() {
        return this.mapMakerPlayerManager;
    }
}
