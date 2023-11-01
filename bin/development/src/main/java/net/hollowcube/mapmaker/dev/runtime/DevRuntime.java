package net.hollowcube.mapmaker.dev.runtime;

import com.google.auto.service.AutoService;
import net.hollowcube.common.ServerRuntime;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ServerRuntime.class)
public class DevRuntime implements ServerRuntime {
    private static final Logger logger = LoggerFactory.getLogger(DevRuntime.class);

    private String version = "3.0.0";
    private String commit = "dev";
    private String minestom = "unknown";
    private String resourcePackHash = "dev";

    public DevRuntime() {
        var version = System.getenv("VERSION");
        if (version != null) this.version = version;

        var commit = System.getenv("COMMIT_SHA");
        if (commit != null) this.commit = commit;

        var resourcePackHash = System.getenv("RESOURCE_PACK_HASH");
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
