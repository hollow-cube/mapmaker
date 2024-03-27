package net.hollowcube.mapmaker.map.setting;

import org.jetbrains.annotations.NotNull;

public enum TimeOfDay {
    NOON,
    SUNSET,
    NIGHT,
    SUNRISE;

    private static final TimeOfDay[] VALUES = values();

    public @NotNull TimeOfDay next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
