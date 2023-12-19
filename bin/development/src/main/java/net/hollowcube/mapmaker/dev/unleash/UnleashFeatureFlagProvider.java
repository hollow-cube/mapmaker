package net.hollowcube.mapmaker.dev.unleash;

import com.google.auto.service.AutoService;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import net.hollowcube.mapmaker.dev.DevServer;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.FlagContext;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureFlagProvider.class)
public class UnleashFeatureFlagProvider implements FeatureFlagProvider {
    private static final boolean DEFAULT_ACTION = false;

    private final Unleash client = DevServer.UNLEASH_INSTANCE;

    @Override
    public boolean test(@NotNull String name, @NotNull FlagContext... context) {
        if (client == null) return DEFAULT_ACTION;
        var unleashContext = UnleashContext.builder();
        for (var ctx : context) unleashContext.addProperty(ctx.name(), ctx.value());
        return client.isEnabled(name, unleashContext.build());
    }
}
