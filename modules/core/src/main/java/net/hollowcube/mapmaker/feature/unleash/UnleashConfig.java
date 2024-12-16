package net.hollowcube.mapmaker.feature.unleash;

import org.jetbrains.annotations.NotNull;

public record UnleashConfig(
        boolean enabled,
        boolean usePosthog,
        @NotNull String address,
        @NotNull String token,
        boolean defaultAction,
        @NotNull String posthogPersonalApiKey,
        @NotNull String posthogProjectApiKey
) {
}
