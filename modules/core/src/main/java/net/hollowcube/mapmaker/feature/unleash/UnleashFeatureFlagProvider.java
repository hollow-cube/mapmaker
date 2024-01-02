package net.hollowcube.mapmaker.feature.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.FlagContext;
import org.jetbrains.annotations.NotNull;

public class UnleashFeatureFlagProvider implements FeatureFlagProvider {
    private static final boolean DEFAULT_ACTION = Boolean.getBoolean("unleash.default");

    private final Unleash client;

    public UnleashFeatureFlagProvider(@NotNull UnleashConfig config) {
        var runtime = ServerRuntime.getRuntime();
        var unleashConfig = io.getunleash.util.UnleashConfig.builder()
                .appName("mapmaker")
                .instanceId(runtime.hostname())
                .unleashAPI(config.address())
                .apiKey(config.token())
                .synchronousFetchOnInitialisation(true)
                .build();
        var mapIds = new MapIdStrategy();
        this.client = new DefaultUnleash(unleashConfig, mapIds);
    }

    @Override
    public boolean test(@NotNull String name, @NotNull FlagContext... context) {
        if (client == null) return DEFAULT_ACTION;
        var unleashContext = UnleashContext.builder();
        for (var ctx : context) {
            if (ctx.name().equals("userId")) {
                unleashContext.userId(ctx.value());
            } else {
                unleashContext.addProperty(ctx.name(), ctx.value());
            }
        }
        return client.isEnabled(name, unleashContext.build());
    }
}
