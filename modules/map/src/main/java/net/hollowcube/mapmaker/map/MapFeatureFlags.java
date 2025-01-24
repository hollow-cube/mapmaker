package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.feature.FeatureFlag;

import java.util.Set;

public final class MapFeatureFlags {

    // Safety features (for stopping execution in case of an issue
    public static final FeatureFlag MAP_NO_REMOTE_UPGRADE = FeatureFlag.of("map.no_remote_upgrade"); // Prevent writing upgraded published worlds
    public static final FeatureFlag TERRAFORM_DISABLE_TASKS = FeatureFlag.of("terraform.disable_tasks");

    // Experiments
    // TODO: Reenable customizable hotbar. Always disabled for now beacuse of issues with items. When closing inventory it will clear your custom items
//    public static final FeatureFlag CUSTOMIZABLE_HOTBAR = FeatureFlag.of("player.customizable_hotbar");
    public static final FeatureFlag CUSTOMIZABLE_HOTBAR = FeatureFlag.never();
    public static final FeatureFlag BIOME_EDITOR = FeatureFlag.of("map.biome_editor");
    public static final FeatureFlag ANIMATION_BUILDER = FeatureFlag.of("map.animation_builder");

    private static final Set<String> ITEM_EDITOR_PLAYERS = Set.of("notmattw", "ontal", "itmg", "sethprg");
    public static final FeatureFlag ITEM_EDITOR = FeatureFlag.of("map.item_editor");
    public static final FeatureFlag EFFECT_MAP_SETTINGS = FeatureFlag.of("map.effect_map_settings"); // Settings for toggle map settings in the checkpoints and status plates

    public static final FeatureFlag MARKER_TOOL = FeatureFlag.of("map.marker_tool");

    // Internal
    public static final FeatureFlag DEBUG_PLAYING_OVERLAY = FeatureFlag.of("debug.playing_overlay");

    private MapFeatureFlags() {
    }

}
