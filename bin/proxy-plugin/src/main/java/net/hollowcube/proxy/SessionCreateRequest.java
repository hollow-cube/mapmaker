package net.hollowcube.proxy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SessionCreateRequest(
        @NotNull String proxy,
        @NotNull String username,
        @NotNull String ip,
        @NotNull Skin skin,
        @Nullable String connectedHost,
        int protocolVersion,
        @NotNull String version
) {

    public record Skin(
            @Nullable String texture,
            @Nullable String signature
    ) {
    }

}
