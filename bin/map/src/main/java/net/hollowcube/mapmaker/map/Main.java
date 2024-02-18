package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class Main {
    public static void main(String[] args) {
        MapServerInitializer.run(MapServerRunner::new);
    }
}
