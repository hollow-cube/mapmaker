package net.hollowcube.map;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class MapFeatureFlags {

    public static FeatureFlag RATE_MAP = FeatureFlag.of("map.rate_map");
    public static FeatureFlag BIOME_EDITOR = FeatureFlag.of("map.biome_editor");
    public static FeatureFlag CHECKPOINT_EDITOR = FeatureFlag.of("map.checkpoint_editor");

    public static FeatureFlag MARKER_TOOL = FeatureFlag.of("map.marker_tool");

    public static FeatureFlag DEBUG_PLAYING_OVERLAY = FeatureFlag.of("debug.playing_overlay");

    private MapFeatureFlags() {
    }

}
