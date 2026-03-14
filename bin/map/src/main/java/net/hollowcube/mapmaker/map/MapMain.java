package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class MapMain {
    static void main(String[] args) {
        MapServerInitializer.run(MapMapServer::new, args);
    }
}
