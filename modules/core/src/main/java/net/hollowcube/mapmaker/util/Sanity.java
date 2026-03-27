package net.hollowcube.mapmaker.util;

import org.jetbrains.annotations.Contract;

public final class Sanity {

    @Contract("false, _ -> fail")
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("sanity: " + message);
        }
    }
}
