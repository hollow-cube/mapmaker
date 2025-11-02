package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class MapFeatureFlags {

    // Experiments
    public static final FeatureFlag BIOME_EDITOR = FeatureFlag.of("map.biome_editor");
    public static final FeatureFlag DISPLAY_ENTITY_EDITOR = FeatureFlag.of("map.display_entity_editor");

    public static final FeatureFlag MARKER_TOOL = FeatureFlag.of("map.marker_tool");

    // WIP Features
    public static final FeatureFlag MACE_ITEM = FeatureFlag.of("map.mace_item");

    // Internal
    public static final FeatureFlag DEBUG_PLAYING_OVERLAY = FeatureFlag.of("debug.playing_overlay");

    private MapFeatureFlags() {
    }

}
