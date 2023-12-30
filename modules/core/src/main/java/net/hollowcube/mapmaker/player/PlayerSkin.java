package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PlayerSkin(
        @Nullable String texture,
        @Nullable String signature
) {

    public @NotNull net.minestom.server.entity.PlayerSkin into() {
        return new net.minestom.server.entity.PlayerSkin(texture, signature);
    }

    @Override
    public String toString() {
        return String.format("PlayerSkin{texture=%b, signature=%b}", texture, signature);
    }
}
