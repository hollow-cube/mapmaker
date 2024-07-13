package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.GlobalConfig;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@SuppressWarnings("unchecked")
public class LocalServer {

    public static void main(String[] args) throws Exception {
        Path projectPath = Path.of("/Users/matt/dev/projects/hollowcube/project-run-2/0_0_cubed_prologue");
//        Path projectPath = Path.of("/Users/matt/Downloads/localmapmakertest");

        MapServerInitializer.run(
                c -> new LocalServerRunner(projectPath, c),
                () -> {
                    var parentConfig = ConfigLoaderV3.loadDefault(args);
                    return new ConfigLoaderV3() {
                        @Override
                        public <C> @NotNull C get(@NotNull Class<C> clazz) {
                            if (clazz == GlobalConfig.class)
                                return (C) new GlobalConfig(true);
                            if (clazz == LocalWorkspace.class)
                                return (C) new LocalWorkspace(projectPath);
                            if (clazz == UnleashConfig.class)
                                return (C) new UnleashConfig(false, "", "", true);
                            return parentConfig.get(clazz);
                        }
                    };
                }
        );
    }

}