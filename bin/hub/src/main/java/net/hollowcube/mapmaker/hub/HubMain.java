package net.hollowcube.mapmaker.hub;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class HubMain {

    static void main(String[] args) {
        System.setProperty("jextract.trace.downcalls", "true");
        System.setProperty("luau.assert-handler", "dump");

        MapServerInitializer.run(HubServer::new, args);
    }

}
