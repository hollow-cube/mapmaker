package net.hollowcube.apiserver.player;

import net.hollowcube.apiserver.db.PlayerData;
import net.hollowcube.apiserver.db.RoleType;
import net.hollowcube.ipc.player.DisplayName;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/// How a player's name is shown: the badge and colour of their effective role, as Go's
/// `computeDisplayName` did it.
public final class DisplayNames {

    // The Hollow Cube organisation account (announcements, official maps): shown by name and in the
    // brand colour, whatever role the row happens to carry.
    private static final UUID ORG_ACCOUNT = UUID.fromString("b571aed9-19f4-4032-9c06-75a4b7cf6c00");
    private static final DisplayName ORG_NAME = new DisplayName(
        List.of(new DisplayName.Part.Username("Hollow Cube", "#3895ff"))
    );

    /// The org account has no `player_data` row, so its name cannot come from one; Go's
    /// display-name endpoint short circuited on the id before it read the player.
    public static @Nullable DisplayName org(UUID id) {
        return id.equals(ORG_ACCOUNT) ? ORG_NAME : null;
    }

    public static DisplayName of(PlayerData row) {
        return of(row.id(), row.username(), row.role(), row.hypercubeEnd());
    }

    public static DisplayName of(
        UUID id,
        String username,
        RoleType role,
        @Nullable Instant hypercubeEnd
    ) {
        var org = org(id);
        if (org != null) return org;
        var effective = Roles.effective(role, hypercubeEnd);
        var badge = badge(effective);
        var name = new DisplayName.Part.Username(username, color(effective));
        return new DisplayName(
            badge == null ? List.of(name) : List.of(new DisplayName.Part.Badge(badge), name)
        );
    }

    private static @Nullable String badge(RoleType effective) {
        return switch (effective) {
            case DEFAULT -> null;
            case HYPERCUBE -> "hypercube/gold";
            case MEDIA, CT_1, MOD_1, DEV_1, CT_2, MOD_2, DEV_2, CT_3, MOD_3, DEV_3 -> effective.pgLabel();
        };
    }

    private static @Nullable String color(RoleType effective) {
        return switch (effective) {
            case DEFAULT -> null;
            case HYPERCUBE -> "#ffb700";
            case MEDIA -> "#cc39e9";
            case CT_1, MOD_1, DEV_1 -> "#46fa32";
            case CT_2, MOD_2, DEV_2 -> "#30fbff";
            case CT_3, MOD_3, DEV_3 -> "#fa4141";
        };
    }

    private DisplayNames() {}
}
