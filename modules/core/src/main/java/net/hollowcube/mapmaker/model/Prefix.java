package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.permission.PlatformPermission;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum Prefix {
    STAFF("Staff", PlatformPermission.STAFF),
    VERIFIED("Verified", PlatformPermission.VERIFIED)
    // The order from top to bottom is the order they will show up next to player's name if they inherit multiple
    ;

    private final String display;
    private final PlatformPermission permission;

    Prefix(String display, PlatformPermission permission) {
        this.display = display;
        this.permission = permission;
    }

    public static String getDisplayFromPerm(PlatformPermission permission) {
        try {
            Prefix prefix = Arrays.stream(Prefix.values()).filter(pre -> pre.permission == permission).findFirst().get();
            return prefix.display;
        } catch (NoSuchElementException e) {
            return "";
        }
    }
}
