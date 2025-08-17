package net.hollowcube.mapmaker.map.feature.common;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.minestom.server.instance.Weather;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class WorldEffectFeatureProvider implements FeatureProvider {
    private static final Weather CLEAR = new Weather(0, 0);
    private static final Weather RAIN = new Weather(1f, 0f);
    private static final Weather THUNDER = new Weather(1f, 1f);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        applyTimeOfDay(world);
        applyWeather(world);
        return true;
    }

    private void applyTimeOfDay(@NotNull MapWorld world) {
        var timeValue = world.map().getSetting(MapSettings.TIME_OF_DAY);
        world.instance().setTime(switch (timeValue) {
            case NOON -> 6000;
            case SUNRISE -> 23000;
            case SUNSET -> 13000;
            case NIGHT -> 18000;
        });
    }

    private void applyWeather(@NotNull MapWorld world) {
        var weatherValue = world.map().getSetting(MapSettings.WEATHER_TYPE);
        world.instance().setWeather(switch (weatherValue) {
            case CLEAR -> CLEAR;
            case RAINING -> RAIN;
            case THUNDERSTORM -> THUNDER;
        }, 1);
    }
}
