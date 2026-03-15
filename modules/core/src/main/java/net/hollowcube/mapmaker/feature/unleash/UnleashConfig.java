package net.hollowcube.mapmaker.feature.unleash;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record UnleashConfig(
        boolean enabled,
        boolean usePosthog,
        String address,
        String token,
        boolean defaultAction,
        @Nullable String posthogPersonalApiKey,
        @Nullable String posthogProjectApiKey
) {

    public boolean hasPosthogCredentials() {
        return posthogPersonalApiKey != null && posthogProjectApiKey != null && !posthogPersonalApiKey.isEmpty() && !posthogProjectApiKey.isEmpty();
    }
}
