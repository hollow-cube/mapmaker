package net.hollowcube.mode;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class Main {

    public static void main(String[] args) {
        MapServerInitializer.run(NewModeServerRunner::new, args);
    }

}
