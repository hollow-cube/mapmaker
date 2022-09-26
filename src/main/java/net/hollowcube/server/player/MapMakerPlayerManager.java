package net.hollowcube.server.player;

public class MapMakerPlayerManager {
    private static MapMakerPlayerLoader mapMakerPlayerLoader;

    public MapMakerPlayerManager() {
        this.mapMakerPlayerLoader = new MapMakerPlayerLoader();
    }

    public static MapMakerPlayerLoader getMapMakerPlayerLoader() {
        return mapMakerPlayerLoader;
    }
}
