package net.hollowcube.mapmaker.feature;

import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;

/**
 * Loaded with SPI
 */
public interface FeatureFlagProvider {

    static @NotNull FeatureFlagProvider current() {
        class Globals {
            private static final FeatureFlagProvider NOOP = (name, context) -> false;
            public static FeatureFlagProvider provider = ServiceLoader.load(FeatureFlagProvider.class)
                    .findFirst().orElse(NOOP);
        }
        return Globals.provider;
    }

    boolean test(@NotNull String name, @NotNull FlagContext... context);

}
