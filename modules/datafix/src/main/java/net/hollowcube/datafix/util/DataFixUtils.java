package net.hollowcube.datafix.util;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;

public final class DataFixUtils {

    public static String namespaced(String value) {
        if (value == null) return value;
        try {
            return Key.key(value).toString();
        } catch (InvalidKeyException ignored) {
            return value;
        }
    }
}
