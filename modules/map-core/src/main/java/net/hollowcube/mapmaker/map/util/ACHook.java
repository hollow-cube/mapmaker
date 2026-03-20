package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.map.MapServer;
import org.jetbrains.annotations.NotNull;

public interface ACHook {

    /// Called right before the server claims to be ready and can accept players
    default void preReady(@NotNull MapServer server) {
    }

}
