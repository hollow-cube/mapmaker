package net.hollowcube.mapmaker.map.setting;

public enum TimeOfDay {
    NOON,
    SUNSET,
    NIGHT,
    SUNRISE;

    private static final TimeOfDay[] VALUES = values();

    public TimeOfDay next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
