package net.hollowcube.mapmaker.map;

import net.hollowcube.map.MapServerRunner;
import net.hollowcube.map2.runtime.MapServerInitializer;

public class Main {
    public static void main(String[] args) {
        MapServerInitializer.run(MapServerRunner::new);
    }
}
