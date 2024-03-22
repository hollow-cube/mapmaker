package net.hollowcube.mapmaker.map.setting;

import org.jetbrains.annotations.NotNull;

public enum TimeOfDay {
    NOON,
    SUNRISE,
    SUNSET,
    NIGHT;

    private static final TimeOfDay[] VALUES = values();

    public @NotNull TimeOfDay next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
