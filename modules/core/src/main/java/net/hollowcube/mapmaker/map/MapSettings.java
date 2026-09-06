package net.hollowcube.mapmaker.map;

import net.hollowcube.ipc.map.*;
import net.hollowcube.ipc.util.Position;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.map.setting.NoSpectateMode;
import net.hollowcube.mapmaker.map.setting.TimeOfDay;
import net.hollowcube.mapmaker.map.setting.WeatherType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minestom.server.ServerFlag;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MapSettings {
    public static final MapSetting<Boolean> BOAT = MapSetting.Bool("boat", false);
    public static final MapSetting<Boolean> ONLY_SPRINT = MapSetting.Bool("only_sprint", false);
    public static final MapSetting<Boolean> NO_SPRINT = MapSetting.Bool("no_sprint", false);
    public static final MapSetting<Boolean> NO_JUMP = MapSetting.Bool("no_jump", false);
    public static final MapSetting<Boolean> NO_SNEAK = MapSetting.Bool("no_sneak", false);

    public static final MapSetting<NoSpectateMode> NO_SPECTATOR = new MapSetting<>("no_spectator", NoSpectateMode.OFF, NoSpectateMode.CODEC);
    public static final MapSetting<Boolean> RESET_IN_WATER = MapSetting.Bool("reset_in_water", false);
    public static final MapSetting<Boolean> RESET_IN_LAVA = MapSetting.Bool("reset_in_lava", false);
    public static final MapSetting<Boolean> NO_RELOG = MapSetting.Bool("no_relog", false);
    public static final MapSetting<Integer> TICK_RATE = MapSetting.Int("tick_rate", ServerFlag.SERVER_TICKS_PER_SECOND, 1, ServerFlag.SERVER_TICKS_PER_SECOND);
    public static final MapSetting<Boolean> NO_TURN = MapSetting.Bool("no_turn", false);
    public static final MapSetting<Integer> DOUBLE_JUMP = MapSetting.Int("double_jump", 0, 0, 100);

    public static final MapSetting<TimeOfDay> TIME_OF_DAY = MapSetting.Enum("time_of_day", TimeOfDay.NOON);
    public static final MapSetting<WeatherType> WEATHER_TYPE = MapSetting.Enum("weather_type", WeatherType.CLEAR);
    public static final MapSetting<Boolean> LIGHTING = MapSetting.Bool("lighting", false);
    public static final MapSetting<String> RESOURCE_PACK = MapSetting.String("resource_pack", "");
    public static final MapSetting<TriState> CAN_SEND_POSE = MapSetting.Enum("can_send_pose", TriState.NOT_SET);

    public static final MapSetting<Boolean>[] TOOLTIP_SETTINGS = new MapSetting[]{
        BOAT, ONLY_SPRINT, NO_SPRINT, NO_JUMP, NO_SNEAK,
    };

    // Weird/one off/experimental settings
    public static final MapSetting<Boolean> PROGRESS_INDEX_ADDITION = MapSetting.Bool("progress_index_addition", false);

    // Internal settings

    // Overrides the map instance size in map-per-server deployments. Should generally not be used a size will be inferred by default.
    public static final MapSetting<String> INSTANCE_SIZE = MapSetting.String("instance_size", "");
    // If set on a parkour map, the map will try to load a script bundle in playing mode.
    public static final MapSetting<Boolean> HAS_SCRIPT_BUNDLE = MapSetting.Bool("has_script_bundle", false);

    public static @NotNull String getNameSafe(MapData.Settings settings) {
        if (settings.name().isEmpty()) return MapData.DEFAULT_NAME;
        return settings.name();
    }

    public static @NotNull Component getNameComponent(MapData.Settings settings) {
        return Component.text(getNameSafe(settings));
    }

    public static @Nullable Material getIcon(MapData.Settings settings) {
        return settings.icon().isEmpty() ? null : Material.fromKey(settings.icon());
    }

    public static Pos getSpawnPoint(MapData.Settings settings) {
        var p = settings.spawnPoint();
        return new Pos(p.x(), p.y(), p.z(), p.yaw(), p.pitch());
    }

    public static Position position(Pos p) {
        return new Position(p.x(), p.y(), p.z(), p.yaw(), p.pitch());
    }

    public static List<MapTags.Tag> getTags(MapData.Settings settings) {
        var tags = new ArrayList<MapTags.Tag>();
        for (var name : settings.tags()) {
            try {
                tags.add(MapTags.Tag.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        return List.copyOf(tags);
    }

    public static boolean hasTag(MapData.Settings settings, MapTags.Tag tag) {
        return settings.tags().contains(tag.name().toLowerCase(Locale.ROOT));
    }

    public static @Nullable ParkourSubVariant getParkourSubVariant(MapData.Settings settings) {
        try {
            return settings.subvariant() == null
                ? null
                : ParkourSubVariant.valueOf(settings.subvariant().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static @Nullable BuildingSubVariant getBuildingSubVariant(MapData.Settings settings) {
        try {
            return settings.subvariant() == null
                ? null
                : BuildingSubVariant.valueOf(settings.subvariant().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static <T> T get(MapData.Settings settings, MapSetting<T> setting) {
        var value = settings.extra().get(setting.key());
        return value == null
            ? setting.defaultValue()
            : setting.codec().decode(Transcoder.JSON, value).orElse(setting.defaultValue());
    }

    public static <T> void set(MapPatch.Builder builder, MapSetting<T> setting, T value) {
        builder.setExtra(setting.key(), setting.codec().encode(Transcoder.JSON, value).orElseThrow());
    }

    public static boolean addTag(MapPatch.Builder builder, MapTags.Tag tag) {
        if (builder.map().settings().variant() == MapVariant.BUILDING && tag.type() == MapTags.TagType.GAMEPLAY)
            throw new IllegalStateException("building maps may not have gameplay tags");
        return builder.addTag(tag.name().toLowerCase(Locale.ROOT));
    }

    public static void setTag(MapPatch.Builder builder, int index, MapTags.Tag tag) {
        var previous = getTags(builder.map().settings()).get(index);
        builder.replaceTag(previous.name().toLowerCase(Locale.ROOT), tag.name().toLowerCase(Locale.ROOT));
    }

    public static boolean removeTag(MapPatch.Builder builder, int index) {
        return removeTag(builder, getTags(builder.map().settings()).get(index));
    }

    public static boolean removeTag(MapPatch.Builder builder, MapTags.Tag tag) {
        return builder.removeTag(tag.name().toLowerCase(Locale.ROOT));
    }

    private MapSettings() {}
}
