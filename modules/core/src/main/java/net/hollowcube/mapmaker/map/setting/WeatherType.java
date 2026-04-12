package net.hollowcube.mapmaker.map.setting;

import net.minestom.server.codec.Codec;
import net.minestom.server.instance.Weather;
import org.jetbrains.annotations.NotNull;

public enum WeatherType {
    CLEAR(new Weather(0, 0)),
    RAINING(new Weather(1f, 0f)),
    THUNDERSTORM(new Weather(1f, 1f));

    private static final WeatherType[] VALUES = values();

    public static final Codec<WeatherType> CODEC = Codec.Enum(WeatherType.class);

    private final Weather weather;

    WeatherType(Weather weather) {
        this.weather = weather;
    }

    public Weather weather() {
        return weather;
    }

    public @NotNull WeatherType next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
