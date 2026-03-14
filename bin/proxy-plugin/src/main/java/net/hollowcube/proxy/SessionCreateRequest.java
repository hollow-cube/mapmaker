package net.hollowcube.proxy;

import org.jetbrains.annotations.Nullable;

public record SessionCreateRequest(
        String proxy,
        String username,
        String ip,
        Skin skin,
        @Nullable String connectedHost,
        int protocolVersion,
        String version
) {

    public record Skin(
            @Nullable String texture,
            @Nullable String signature
    ) {
    }

}
