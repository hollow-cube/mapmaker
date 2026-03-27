package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class DevMain {

    static void main(String[] args) throws Exception {
        MapServerInitializer.run(DevServer::new, args);
    }

}
