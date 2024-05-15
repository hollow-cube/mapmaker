package net.hollowcube.mapmaker.feature.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.event.UnleashReady;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.repository.FeatureToggleResponse;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.event.FeatureFlagReloadEvent;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.FlagContext;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.util.Shutdowner;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class UnleashFeatureFlagProvider implements FeatureFlagProvider {
    private final Unleash client;
    private final boolean defaultAction;

    public UnleashFeatureFlagProvider(@NotNull UnleashConfig config, @NotNull Shutdowner shutdowner) {
        var runtime = ServerRuntime.getRuntime();
        var unleashConfig = io.getunleash.util.UnleashConfig.builder()
                .appName("mapmaker")
                .instanceId(runtime.hostname())
                .unleashAPI(config.address())
                .apiKey(config.token())
                .synchronousFetchOnInitialisation(true)
                .subscriber(new UnleashSubscriber() {
                    private boolean isReady = false;

                    @Override
                    public void onReady(UnleashReady unleashReady) {
                        isReady = true; // Used to skip the initial refresh
                    }

                    @Override
                    public void togglesFetched(@NotNull FeatureToggleResponse response) {
                        if (!isReady || response.getStatus() != FeatureToggleResponse.Status.CHANGED)
                            return;

                        FutureUtil.submitVirtual(() -> EventDispatcher.call(new FeatureFlagReloadEvent()));
                    }
                })
                .build();
        var mapIds = new MapIdStrategy();
        this.client = new DefaultUnleash(unleashConfig, mapIds);
        shutdowner.queue(client::shutdown);
        this.defaultAction = config.defaultAction();
    }

    @Override
    public boolean test(@NotNull String name, @NotNull Object... context) {
        if (client == null) return defaultAction;
        var unleashContext = UnleashContext.builder();
        for (var ctx : context) {
            switch (ctx) {
                case FlagContext flag -> {
                    if (flag.name().equals("userId")) {
                        unleashContext.userId(flag.value());
                    } else {
                        unleashContext.addProperty(flag.name(), flag.value());
                    }
                }
                case Player player -> unleashContext.userId(PlayerDataV2.fromPlayer(player).id());
                case PlayerDataV2 pd -> unleashContext.userId(pd.id());
                case MapData map -> unleashContext.addProperty("mapId", map.id());
                default -> {
                }
            }
        }
        return client.isEnabled(name, unleashContext.build());
    }
}
