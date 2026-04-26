package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.feature.FeatureFlag;

public final class CoreFeatureFlags {

    public static FeatureFlag MAP_DISABLE_ALL = FeatureFlag.of("map.disable_all");

    public static FeatureFlag SPAWN_MAP_ACCESS = FeatureFlag.of("spawn_map_access");
    public static FeatureFlag ORGANIZATIONS = FeatureFlag.of("organizations");

    public static FeatureFlag SERVER_STAT_OVERLAY = FeatureFlag.of("debug.server_stat_overlay");

    public static FeatureFlag NO_SPEC_ACCESS = FeatureFlag.of("no_spec_access");

}
