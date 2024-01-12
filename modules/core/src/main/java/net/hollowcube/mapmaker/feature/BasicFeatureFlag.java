package net.hollowcube.mapmaker.feature;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

record BasicFeatureFlag(@NotNull String name) implements FeatureFlag {

    @Override
    public boolean test(@NotNull Object... context) {
        try {
            return FeatureFlagProvider.current().test(name, context);
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            return false;
        }
    }

}
