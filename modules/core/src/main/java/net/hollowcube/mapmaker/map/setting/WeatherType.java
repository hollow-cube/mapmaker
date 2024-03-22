package net.hollowcube.mapmaker.map.setting;

import org.jetbrains.annotations.NotNull;

public enum WeatherType {
    CLEAR, RAINING, THUNDERSTORM;


    private static final WeatherType[] VALUES = values();

    public @NotNull WeatherType next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
