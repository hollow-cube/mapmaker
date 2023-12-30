package net.hollowcube.mapmaker.feature;

final class Globals {
    static final FeatureFlagProvider NOOP = (name, context) -> false;
    static FeatureFlagProvider provider = NOOP;
}
