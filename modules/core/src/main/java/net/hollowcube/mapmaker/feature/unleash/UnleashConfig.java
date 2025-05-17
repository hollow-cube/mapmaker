package net.hollowcube.mapmaker.feature.unleash;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record UnleashConfig(
        boolean enabled,
        boolean usePosthog,
        @NotNull String address,
        @NotNull String token,
        boolean defaultAction,
        @NotNull String posthogPersonalApiKey,
        @NotNull String posthogProjectApiKey
) {

    public boolean hasPosthogCredentials() {
        return posthogPersonalApiKey != null && posthogProjectApiKey != null && !posthogPersonalApiKey.isEmpty() && !posthogProjectApiKey.isEmpty();
    }
}
