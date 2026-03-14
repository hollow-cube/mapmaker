package net.hollowcube.common.util;

import net.hollowcube.schem.util.Axis;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Utility class for handling block properties
 */
public class PropertyUtil {
    private PropertyUtil() {
    }

    @Contract("null, _ -> null")
    private static <T extends Enum<T>> @Nullable T getEnum(@Nullable String value, Class<T> enumClass) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    public static @Nullable Direction getFacing(Map<String, String> properties) {
        return OpUtils.map(properties.get("facing"), facing -> getEnum(facing, Direction.class));
    }

    public static @Nullable Axis getAxis(Map<String, String> properties) {
        return OpUtils.map(properties.get("axis"), axis -> getEnum(axis, Axis.class));
    }

    public static @Nullable Integer getRotation(Map<String, String> properties) {
        try {
            return OpUtils.map(properties.get("rotation"), Integer::parseInt);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static @Nullable Map<Direction, String> getConnections(Map<String, String> properties) {
        String north = properties.get("north");
        String east = properties.get("east");
        String south = properties.get("south");
        String west = properties.get("west");
        if (north == null || east == null || south == null || west == null) return null;
        return Map.of(
                Direction.NORTH, north,
                Direction.EAST, east,
                Direction.SOUTH, south,
                Direction.WEST, west
        );
    }
}
