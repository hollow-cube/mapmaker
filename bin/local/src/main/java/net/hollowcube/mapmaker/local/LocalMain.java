package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class LocalMain {

    static void main(String[] args) {
        MapServerInitializer.run(LocalMapServer::new, args);
    }
}
