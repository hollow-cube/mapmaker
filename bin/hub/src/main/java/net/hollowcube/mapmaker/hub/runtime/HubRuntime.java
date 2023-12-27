package net.hollowcube.mapmaker.hub.runtime;

import com.google.auto.service.AutoService;
import net.hollowcube.common.ServerRuntime;
import org.jetbrains.annotations.NotNull;

@AutoService(ServerRuntime.class)
public class HubRuntime implements ServerRuntime {
    private String version = "3.0.0";
    private String commit = "dev";
    private String minestom = "unknown";
    private String resourcePackHash = "dev";

    public HubRuntime() {
        var version = System.getenv("MAPMAKER_VERSION");
        if (version != null) this.version = version;

        var commit = System.getenv("MAPMAKER_COMMIT_SHA");
        if (commit != null) this.commit = commit;

        var resourcePackHash = System.getenv("MAPMAKER_RESOURCE_PACK_HASH");
        if (resourcePackHash != null) this.resourcePackHash = resourcePackHash;
    }

    @Override
    public @NotNull String version() {
        return version;
    }

    @Override
    public @NotNull String commit() {
        return commit;
    }

    @Override
    public @NotNull String minestom() {
        return minestom;
    }


    public @NotNull String resourcePackSha1() {
        return resourcePackHash;
    }

}
