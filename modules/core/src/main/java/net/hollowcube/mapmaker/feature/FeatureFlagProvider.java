package net.hollowcube.mapmaker.feature;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Loaded with SPI
 */
public interface FeatureFlagProvider {

    static void replaceGlobals(@NotNull FeatureFlagProvider provider) {
        Globals.provider = Objects.requireNonNull(provider);
    }

    static @NotNull FeatureFlagProvider current() {
        return Globals.provider;
    }

    boolean test(@NotNull String name, @NotNull Object... context);

    default void close() {
    }

}
