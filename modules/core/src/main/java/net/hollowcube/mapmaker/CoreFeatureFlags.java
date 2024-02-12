package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class CoreFeatureFlags {

    public static FeatureFlag STORE = FeatureFlag.of("store");
    public static FeatureFlag MAP_REPORTS = FeatureFlag.of("map_reports");
    public static FeatureFlag COSMETICS = FeatureFlag.of("cosmetics");
    public static FeatureFlag BACKPACK = FeatureFlag.of("backpack");

    public static FeatureFlag SPAWN_MAP_ACCESS = FeatureFlag.of("spawn_map_access");
    public static FeatureFlag ORGANIZATIONS = FeatureFlag.of("organizations");

}
