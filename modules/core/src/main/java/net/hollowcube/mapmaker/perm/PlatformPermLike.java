package net.hollowcube.mapmaker.perm;

import org.jetbrains.annotations.NotNull;

public interface PlatformPermLike {

    static @NotNull PlatformPermLike of(@NotNull String permName) {
        record Impl(@NotNull String permName) implements PlatformPermLike {
        }
        return new Impl(permName);
    }

    @NotNull String permName();

}
