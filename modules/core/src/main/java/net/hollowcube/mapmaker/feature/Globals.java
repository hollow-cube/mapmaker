package net.hollowcube.mapmaker.feature;

final class Globals {
    static final FeatureFlagProvider NOOP = (name, context) -> Boolean.getBoolean("unleash.default");
    static FeatureFlagProvider provider = NOOP;
}
