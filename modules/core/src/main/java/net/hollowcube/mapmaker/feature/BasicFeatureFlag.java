package net.hollowcube.mapmaker.feature;

import net.hollowcube.mapmaker.ExceptionReporter;
import org.jetbrains.annotations.NotNull;

record BasicFeatureFlag(@NotNull String name) implements FeatureFlag {

    @Override
    public boolean test(@NotNull Object... context) {
        try {
            return FeatureFlagProvider.current().test(name, context);
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
            return false;
        }
    }

}
