package net.hollowcube.apiserver.player;

import net.hollowcube.apiserver.db.RoleType;
import net.hollowcube.ipc.map.MapSize;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/// Go's `pkg/player` permission flags and the limits that hang off them, resolved from the two
/// columns that decide them. The flag values are what the game servers already read off a
/// player's `permissions`, so they cannot change.
public final class Roles {

    public static final long EXTENDED_LIMITS = 1;
    public static final long GENERIC_STAFF = 1L << 63;

    private static final int FREE_MAP_SLOTS = 2;
    private static final int EXTENDED_MAP_SLOTS = 3;
    private static final int FREE_BUILDER_SLOTS = 1;
    private static final int MAX_BUILDER_SLOTS = 4;

    /// An active hypercube promotes a default player and nobody else; a staff role is already
    /// everything hypercube grants.
    public static RoleType effective(RoleType role, @Nullable Instant hypercubeEnd) {
        var hasHypercube = hypercubeEnd != null && hypercubeEnd.isAfter(Instant.now());
        return role == RoleType.DEFAULT && hasHypercube ? RoleType.HYPERCUBE : role;
    }

    public static long flags(RoleType role, @Nullable Instant hypercubeEnd) {
        return switch (effective(role, hypercubeEnd)) {
            case DEFAULT -> 0;
            case HYPERCUBE, MEDIA -> EXTENDED_LIMITS;
            case CT_1, MOD_1, DEV_1, CT_2, MOD_2, DEV_2, CT_3, MOD_3, DEV_3 -> EXTENDED_LIMITS
                | GENERIC_STAFF;
        };
    }

    public static int mapSlots(int extraMapSlots, long flags) {
        var slots = FREE_MAP_SLOTS + extraMapSlots;
        if ((flags & EXTENDED_LIMITS) != 0) slots += EXTENDED_MAP_SLOTS;
        return slots;
    }

    /// Owner included.
    public static int builderSlots(int mapBuilders, long flags) {
        if ((flags & EXTENDED_LIMITS) != 0) return MAX_BUILDER_SLOTS;
        return Math.min(FREE_BUILDER_SLOTS + mapBuilders, MAX_BUILDER_SLOTS);
    }

    /// `maxMapSize` is the column: the largest size bought from the store, as a [MapSize] id.
    public static MapSize maxMapSize(int maxMapSize, long flags) {
        var bought = MapSize.fromId(maxMapSize);
        if ((flags & EXTENDED_LIMITS) != 0 && MapSize.MASSIVE.unlocks(bought))
            return MapSize.MASSIVE;
        return bought;
    }

    public static boolean hasExtendedLimits(RoleType role, @Nullable Instant hypercubeEnd) {
        return (flags(role, hypercubeEnd) & EXTENDED_LIMITS) != 0;
    }

    public static boolean isStaff(RoleType role, @Nullable Instant hypercubeEnd) {
        return (flags(role, hypercubeEnd) & GENERIC_STAFF) != 0;
    }

    private Roles() {}
}
