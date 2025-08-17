package net.hollowcube.mapmaker.isolate;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class IsolateMain {
    public static String[] args;

    public static void main(String[] args) {
        IsolateMain.args = args;
        MapServerInitializer.run(MapIsolateServer::new, args);
    }
}
