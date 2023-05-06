package net.hollowcube.common.facet;

import net.hollowcube.common.config.ConfigProvider;
import net.minestom.server.ServerProcess;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * A facet of the server, intended to be used with {@link com.google.auto.service.AutoService}.
 * <p>
 * Hook is called on server start and should handle initialization logic.
 */
@SuppressWarnings("UnstableApiUsage")
public interface Facet {
    /**
     * Setup method called on server start. Called from within a virtual thread, so may block.
     *
     * @param server The minestom server
     */
    @Blocking
    void hook(@NotNull ServerProcess server, @NotNull ConfigProvider config);
}
