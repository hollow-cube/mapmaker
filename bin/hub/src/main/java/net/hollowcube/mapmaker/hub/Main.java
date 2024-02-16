package net.hollowcube.mapmaker.hub;

import net.hollowcube.map2.runtime.MapServerInitializer;

public class Main {

    public static void main(String[] args) {
        MapServerInitializer.run(HubServerRunner::new);
    }

}
