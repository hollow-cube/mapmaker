package net.hollowcube.mapmaker.feature;

import java.util.Objects;

/**
 * Loaded with SPI
 */
public interface FeatureFlagProvider {

    static void replaceGlobals(FeatureFlagProvider provider) {
        Globals.provider = Objects.requireNonNull(provider);
    }

    static FeatureFlagProvider current() {
        return Globals.provider;
    }

    boolean test(String name, Object... context);

    default void close() {
    }

}
