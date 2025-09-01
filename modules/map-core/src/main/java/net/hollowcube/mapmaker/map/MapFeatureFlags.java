package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class MapFeatureFlags {

    // Experiments
    public static final FeatureFlag BIOME_EDITOR = FeatureFlag.of("map.biome_editor");
    public static final FeatureFlag DISPLAY_ENTITY_EDITOR = FeatureFlag.of("map.display_entity_editor");
    public static final FeatureFlag NO_POSE_CHANGES = FeatureFlag.of("map.no_pose_changes");

    public static final FeatureFlag MARKER_TOOL = FeatureFlag.of("map.marker_tool");

    // WIP Features
    public static final FeatureFlag SPEC_GAMEPLAY_SETTINGS = FeatureFlag.of("map.spec_gameplay_settings");

    // Internal
    public static final FeatureFlag DEBUG_PLAYING_OVERLAY = FeatureFlag.of("debug.playing_overlay");

    private MapFeatureFlags() {
    }

}
