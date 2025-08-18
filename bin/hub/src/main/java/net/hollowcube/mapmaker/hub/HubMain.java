package net.hollowcube.mapmaker.hub;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class HubMain {

    public static void main(String[] args) {
        MapServerInitializer.run(HubServer::new, args);
    }

}
