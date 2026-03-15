package net.hollowcube.mapmaker.feature;

import net.hollowcube.mapmaker.ExceptionReporter;

record BasicFeatureFlag(String name) implements FeatureFlag {

    @Override
    public boolean test(Object... context) {
        try {
            return FeatureFlagProvider.current().test(name, context);
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
            return false;
        }
    }

}
