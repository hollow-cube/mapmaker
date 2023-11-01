package net.hollowcube.mapmaker.dev.runtime;

import com.google.auto.service.AutoService;
import net.hollowcube.common.ServerRuntime;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@AutoService(ServerRuntime.class)
public class DevRuntime implements ServerRuntime {
    private static final Logger logger = LoggerFactory.getLogger(DevRuntime.class);

    private String version = "3.0.0";
    private String commit = "dev";
    private String minestom = "unknown";
    private String resourcePackHash = "dev";

    public DevRuntime() {
        try (var is = getClass().getResourceAsStream("/runtime.properties")) {
            if (is == null) return;

            var props = new Properties();
            props.load(is);

            version = props.getOrDefault("version", "3.0.0").toString();
            commit = props.getOrDefault("commit_sha", "dev").toString();
            minestom = props.getOrDefault("minestom", "unknown").toString();
            resourcePackHash = props.getOrDefault("resource_pack_hash", "dev").toString();
        } catch (Exception e) {
            logger.warn("Failed to load runtime properties", e);
        }
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
