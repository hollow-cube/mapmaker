package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.GlobalConfig;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@SuppressWarnings("unchecked")
public class LocalServer {

    public static void main(String[] args) {
        Path projectPath = Path.of("/Users/matt/Downloads/hublightingtest");

        MapServerInitializer.run(LocalServerRunner::new, () -> {
            var parentConfig = ConfigLoaderV3.loadDefault(args);
            return new ConfigLoaderV3() {
                @Override
                public <C> @NotNull C get(@NotNull Class<C> clazz) {
                    if (clazz == GlobalConfig.class)
                        return (C) new GlobalConfig(true);
                    return parentConfig.get(clazz);
                }

                @Override
                public void dump() {

                }
            };
        });
    }

}