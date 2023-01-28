package net.hollowcube.common.facet;

import com.google.common.util.concurrent.ListenableFuture;
import net.minestom.server.ServerProcess;
import org.jetbrains.annotations.NotNull;

/**
 * A facet of the server, intended to be used with {@link com.google.auto.service.AutoService}.
 * <p>
 * Hook is called on server start and should handle initialization logic.
 */
@SuppressWarnings("UnstableApiUsage")
public interface Facet {
    @NotNull ListenableFuture<Void> hook(@NotNull ServerProcess server);
}
