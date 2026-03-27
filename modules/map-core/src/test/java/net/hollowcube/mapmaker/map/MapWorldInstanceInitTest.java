package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.setting.TimeOfDay;
import net.hollowcube.mapmaker.map.setting.WeatherType;
import net.minestom.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapWorldInstanceInitTest {

    static {
        MinecraftServer.init();
    }

    @Test
    void testWorldBorderInit() {
        var map = new MapData();
        map.settings().setSize(MapSize.COLOSSAL);

        var world = new NoopMapWorld(map);

        assertEquals(1200, world.instance().getWorldBorder().diameter());
    }

    @Test
    void testWeatherSetting() {
        var map = new MapData();
        map.setSetting(MapSettings.WEATHER_TYPE, WeatherType.THUNDERSTORM);

        var world = new NoopMapWorld(map);

        var weather = world.instance().getWeather();
        assertEquals(1, weather.rainLevel());
        assertEquals(1, weather.thunderLevel());
    }

    @Test
    void testTimeOfDaySetting() {
        var map = new MapData();
        map.setSetting(MapSettings.TIME_OF_DAY, TimeOfDay.SUNSET);

        var world = new NoopMapWorld(map);

        assertEquals(13000, world.instance().getTime());
    }

    private static class NoopMapWorld extends AbstractMapWorld {

        public NoopMapWorld(MapData map) {
            super(new TestMapServer(false), map, makeMapInstance(map, 't'));
        }

    }
}
