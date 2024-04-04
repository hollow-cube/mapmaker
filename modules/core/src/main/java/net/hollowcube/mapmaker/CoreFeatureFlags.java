package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class CoreFeatureFlags {

    public static FeatureFlag MAP_DISABLE_ALL = FeatureFlag.of("map.disable_all");

    public static FeatureFlag STORE = FeatureFlag.of("store");
    public static FeatureFlag MAP_REPORTS = FeatureFlag.of("map_reports");

    public static FeatureFlag SPAWN_MAP_ACCESS = FeatureFlag.of("spawn_map_access");
    public static FeatureFlag ORGANIZATIONS = FeatureFlag.of("organizations");

}
