package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalMain {

    static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Must provide map directory as last argument!");
            return;
        }
        var mapDirectory = Path.of(args[args.length - 1]);
        if (!Files.isDirectory(mapDirectory)) {
            System.out.println("Map directory does not exist: " + mapDirectory);
            return;
        }
        var realPath = mapDirectory.toRealPath();
        MapServerInitializer.run(c -> new LocalMapServer(c, realPath), args);
    }
}
