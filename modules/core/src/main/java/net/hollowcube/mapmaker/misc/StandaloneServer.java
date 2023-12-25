package net.hollowcube.mapmaker.misc;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import org.eclipse.microprofile.health.HealthCheck;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface StandaloneServer {

    @NotNull Collection<@NotNull HealthCheck> readinessChecks();

    void start(@NotNull ConfigLoaderV3 config);

    void handleHttpShutdown(@NotNull ServerRequest serverRequest, @NotNull ServerResponse serverResponse);
}
