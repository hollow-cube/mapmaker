package net.hollowcube.mapmaker.isolate;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class IsolateMain {
    public static String[] args;

    public static void main(String[] args) {
        if (args.length > 0 && "noop".equals(args[0])) {
            System.out.println("Exiting because of noop argument.");
            System.exit(0);
        }

        IsolateMain.args = args;
        MapServerInitializer.run(MapIsolateServer::new, args);
    }
}
