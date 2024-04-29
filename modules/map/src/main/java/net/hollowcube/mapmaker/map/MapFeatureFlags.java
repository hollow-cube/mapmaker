package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class MapFeatureFlags {

    // Safety features (for stopping execution in case of an issue
    public static FeatureFlag MAP_NO_REMOTE_UPGRADE = FeatureFlag.of("map.no_remote_upgrade"); // Prevent writing upgraded published worlds
    public static FeatureFlag TERRAFORM_DISABLE_TASKS = FeatureFlag.of("terraform.disable_tasks");

    // Experiments
    public static FeatureFlag BIOME_EDITOR = FeatureFlag.of("map.biome_editor");
    public static FeatureFlag ANIMATION_BUILDER = FeatureFlag.of("map.animation_builder");

    public static FeatureFlag MARKER_TOOL = FeatureFlag.of("map.marker_tool");

    // Internal
    public static FeatureFlag DEBUG_PLAYING_OVERLAY = FeatureFlag.of("debug.playing_overlay");

    private MapFeatureFlags() {
    }

}
