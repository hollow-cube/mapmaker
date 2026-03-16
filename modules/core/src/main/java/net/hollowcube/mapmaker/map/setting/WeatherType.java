package net.hollowcube.mapmaker.map.setting;

public enum WeatherType {
    CLEAR, RAINING, THUNDERSTORM;


    private static final WeatherType[] VALUES = values();

    public WeatherType next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
