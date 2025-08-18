package net.hollowcube.example;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class Main {

    public static void main(String[] args) {
        MapServerInitializer.run(ExampleServer::new, args);
    }

}
