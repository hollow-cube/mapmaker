package net.hollowcube.mapmaker.perm;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * WARNING!! This enum should be kept in sync with the spicedb schema in zed/mapmaker.zed.
 */
public enum PlatformPerm implements PlatformPermLike {

    // Platform Map Permissions
    MAP_ADMIN,

    // Punishments
    VIEW_PUNISHMENTS,
    KICK_PLAYER,
    MUTE_PLAYER,
    BAN_PLAYER,

    // Misc Moderation
    VANISH,
    SEE_VANISHED,

    ;

    @Override
    public @NotNull String permName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
