package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.GlobalConfig;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

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
        MapServerInitializer.run(c -> new LocalMapServer(new ConfigLoaderV3() {
            @Override
            public @NonNull <C> C get(@NotNull Class<C> clazz) {
                if (clazz.equals(GlobalConfig.class))
                    return (C) new GlobalConfig(true);
                return c.get(clazz);
            }

            @Override
            public void dump() {
                c.dump();
            }
        }, realPath), args);
    }
}
