package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class DevServer {
    
    public static void main(String[] args) throws Exception {
        MapServerInitializer.run(DevServerRunner::new, args);
    }
}
