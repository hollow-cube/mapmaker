package net.hollowcube.server;

import omega.mapmaker.player.MapMakerPlayerManager;
import omega.mapmaker.worldmanager.WorldInstanceManager;

public class MapMaker {
    private static MapMaker instance;

    private WorldInstanceManager worldInstanceManager;
    private MapMakerPlayerManager mapMakerPlayerManager;

    public MapMaker() {
        this.instance = this;

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
