package net.hollowcube.mapmaker.dev;

import com.google.auto.service.AutoService;
import net.hollowcube.common.ServerRuntime;
import org.jetbrains.annotations.NotNull;

@AutoService(ServerRuntime.class)
public class DevRuntime implements ServerRuntime {

    @Override
    public @NotNull String version() {
        return "{MAPMAKER_VERSION}";
    }

    @Override
    public @NotNull String commit() {
        return "{MAPMAKER_COMMIT}";
    }

    @Override
    public @NotNull String minestom() {
        return "{MINESTOM_VERSION}";
    }


    public @NotNull String resourcePackSha1() {
        return "{RESOURCE_PACK_SHA}";
    }

}
