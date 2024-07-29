package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class MapFeatureFlags {

    // Safety features (for stopping execution in case of an issue
    public static final FeatureFlag MAP_NO_REMOTE_UPGRADE = net.hollowcube.mapmaker.feature.FeatureFlag.of("map.no_remote_upgrade"); // Prevent writing upgraded published worlds
    public static final FeatureFlag TERRAFORM_DISABLE_TASKS = net.hollowcube.mapmaker.feature.FeatureFlag.of("terraform.disable_tasks");

    // Experiments
    public static final FeatureFlag CUSTOMIZABLE_HOTBAR = FeatureFlag.of("player.customizable_hotbar");
    public static final FeatureFlag BIOME_EDITOR = FeatureFlag.of("map.biome_editor");
    public static final FeatureFlag ANIMATION_BUILDER = FeatureFlag.of("map.animation_builder");

    public static final FeatureFlag MARKER_TOOL = FeatureFlag.of("map.marker_tool");

    // Internal
    public static final FeatureFlag DEBUG_PLAYING_OVERLAY = FeatureFlag.of("debug.playing_overlay");

    private MapFeatureFlags() {
    }

}
