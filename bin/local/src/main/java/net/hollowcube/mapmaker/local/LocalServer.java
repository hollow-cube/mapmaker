package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.GlobalConfig;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.local.proj.Workspace;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@SuppressWarnings("unchecked")
public class LocalServer {

    public static void main(String[] args) throws Exception {
        Path workspacePath = Path.of("/Users/matt/dev/projects/hollowcube/project-run-2");
        Workspace workspace = Workspace.read(workspacePath);

        LocalWorkspace wsConf = new LocalWorkspace(
                workspacePath,
                workspacePath.resolve(workspace.activeProject()),
                workspacePath.resolve("schematics")
        );

        MapServerInitializer.run(
                LocalServerRunner::new,
                () -> {
                    var parentConfig = ConfigLoaderV3.loadDefault(args);
                    return new ConfigLoaderV3() {
                        @Override
                        public <C> @NotNull C get(@NotNull Class<C> clazz) {
                            if (clazz == GlobalConfig.class)
                                return (C) new GlobalConfig(true);
                            if (clazz == LocalWorkspace.class)
                                return (C) wsConf;
                            if (clazz == UnleashConfig.class)
                                return (C) new UnleashConfig(false, "", "", true);
                            return parentConfig.get(clazz);
                        }
                    };
                }
        );
    }

}