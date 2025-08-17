package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.minestom.server.codec.Codec;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class SavedMapSettings {

    @SuppressWarnings("unchecked")
    public static final Codec<SavedMapSettings> CODEC = ExtraCodecs.dispatchedMap(MapSetting.CODEC, s -> (Codec<Object>) s.codec())
            .transform(SavedMapSettings::new, settings -> settings.settings);

    private final Map<MapSetting<?>, Object> settings = new HashMap<>();

    public static final SavedMapSettings EMPTY = new SavedMapSettings();

    public SavedMapSettings() {
    }

    private SavedMapSettings(Map<MapSetting<?>, ?> settings) {
        this.settings.putAll(settings);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(MapSetting<T> setting, MapSettings settings) {
        return (T) this.settings.getOrDefault(setting, settings.get(setting));
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrNull(MapSetting<T> setting) {
        return (T) this.settings.get(setting);
    }

    public <T> SavedMapSettings with(MapSetting<T> setting, T value) {
        SavedMapSettings copy = new SavedMapSettings(this.settings);
        copy.settings.put(setting, value);
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SavedMapSettings{");
        for (Map.Entry<MapSetting<?>, Object> entry : this.settings.entrySet()) {
            builder.append(entry.getKey().key()).append("=").append(entry.getValue()).append(", ");
        }
        builder.append("}");
        return builder.toString();
    }
}
