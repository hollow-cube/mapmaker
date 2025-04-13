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

    public static String dyeColorIdToName(int id) {
        return switch (id) {
            case 1 -> "orange";
            case 2 -> "magenta";
            case 3 -> "light_blue";
            case 4 -> "yellow";
            case 5 -> "lime";
            case 6 -> "pink";
            case 7 -> "gray";
            case 8 -> "light_gray";
            case 9 -> "cyan";
            case 10 -> "purple";
            case 11 -> "blue";
            case 12 -> "brown";
            case 13 -> "green";
            case 14 -> "red";
            case 15 -> "black";
            default -> "white";
        };
    }
}
