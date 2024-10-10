package net.hollowcube.common.util;

import net.minestom.server.entity.PlayerSkin;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MojangUtil {

    @Blocking
    public static @Nullable PlayerSkin getSkinFromUuid(@NotNull String uuid) {
        FutureUtil.assertThread();
        return PlayerSkin.fromUuid(uuid);
    }

    @Blocking
    public static @Nullable PlayerSkin getSkinFromUsername(@NotNull String username) {
        FutureUtil.assertThread();
        return PlayerSkin.fromUsername(username);
    }

}
