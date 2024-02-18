package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import org.jetbrains.annotations.Blocking;

@Blocking
public class Main {
    public static void main(String[] args) {
        MapServerInitializer.run(MapServerRunner::new, args);
    }
}
