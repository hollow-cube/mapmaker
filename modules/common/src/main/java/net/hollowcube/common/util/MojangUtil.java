package net.hollowcube.common.util;

import net.minestom.server.entity.PlayerSkin;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

public final class MojangUtil {
    private MojangUtil() {
    }

    @Blocking
    public static @Nullable PlayerSkin getSkinFromUuid(String uuid) {
        FutureUtil.assertThread();
        return PlayerSkin.fromUuid(uuid);
    }

    @Blocking
    public static @Nullable PlayerSkin getSkinFromUsername(String username) {
        FutureUtil.assertThread();
        return PlayerSkin.fromUsername(username);
    }

}
