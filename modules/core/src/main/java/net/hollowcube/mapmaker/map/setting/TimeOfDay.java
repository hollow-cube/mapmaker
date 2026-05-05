package net.hollowcube.mapmaker.map.setting;

import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.NotNull;

public enum TimeOfDay {
    NOON(6000),
    SUNSET(12500),
    NIGHT(18000),
    SUNRISE(0);

    private static final TimeOfDay[] VALUES = values();

    public static final Codec<TimeOfDay> CODEC = Codec.Enum(TimeOfDay.class);

    private final int time;

    TimeOfDay(int time) {
        this.time = time;
    }

    public int time() {
        return time;
    }

    public @NotNull TimeOfDay next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
