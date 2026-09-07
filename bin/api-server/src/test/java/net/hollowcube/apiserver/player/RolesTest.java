package net.hollowcube.apiserver.player;

import net.hollowcube.apiserver.db.RoleType;
import net.hollowcube.ipc.player.DisplayName;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Go's `pkg/player/permissions.go` table, pinned.
class RolesTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    static Stream<Arguments> roles() {
        var staff = Roles.EXTENDED_LIMITS | Roles.GENERIC_STAFF;
        return Stream.of(
            Arguments.of("default", 0L, null, null),
            Arguments.of("hypercube", Roles.EXTENDED_LIMITS, "hypercube/gold", "#ffb700"),
            Arguments.of("media", Roles.EXTENDED_LIMITS, "media", "#cc39e9"),
            Arguments.of("ct_1", staff, "ct_1", "#46fa32"),
            Arguments.of("mod_2", staff, "mod_2", "#30fbff"),
            Arguments.of("dev_3", staff, "dev_3", "#fa4141")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roles")
    void flags_andDisplayName_perRole(
        String role,
        long flags,
        @Nullable String badge,
        @Nullable String color
    ) {
        var type = RoleType.fromPg(role);
        assertEquals(flags, Roles.flags(type, null));
        var name = DisplayNames.of(ID, "Someone", type, null);
        assertEquals(badge, name.badge());
        var username = assertInstanceOf(DisplayName.Part.Username.class, name.parts().getLast());
        assertEquals("Someone", username.text());
        assertEquals(color, username.color());
    }

    @Test
    void effective_hypercubePromotesDefaultOnly() {
        var later = Instant.now().plus(Duration.ofDays(1));
        var earlier = Instant.now().minus(Duration.ofDays(1));
        assertEquals(RoleType.HYPERCUBE, Roles.effective(RoleType.DEFAULT, later));
        assertEquals(RoleType.DEFAULT, Roles.effective(RoleType.DEFAULT, earlier));
        assertEquals(RoleType.MOD_1, Roles.effective(RoleType.MOD_1, later));
        assertEquals(Roles.EXTENDED_LIMITS, Roles.flags(RoleType.DEFAULT, later));
    }

    @Test
    void displayName_orgAccount() {
        var name = DisplayNames.of(
            UUID.fromString("b571aed9-19f4-4032-9c06-75a4b7cf6c00"),
            "hc_org",
            RoleType.DEV_3,
            null
        );
        assertNull(name.badge());
        assertEquals("Hollow Cube", name.username());
        assertEquals(
            new DisplayName.Part.Username("Hollow Cube", "#3895ff"),
            name.parts().getFirst()
        );
    }

    @Test
    void slots_extendedLimitsAddToBought() {
        assertEquals(2, Roles.mapSlots(0, 0));
        assertEquals(4, Roles.mapSlots(2, 0));
        assertEquals(7, Roles.mapSlots(2, Roles.EXTENDED_LIMITS));
        assertEquals(1, Roles.builderSlots(0, 0));
        assertEquals(3, Roles.builderSlots(2, 0));
        assertEquals(4, Roles.builderSlots(9, 0));
        assertEquals(4, Roles.builderSlots(0, Roles.EXTENDED_LIMITS));
    }
}
