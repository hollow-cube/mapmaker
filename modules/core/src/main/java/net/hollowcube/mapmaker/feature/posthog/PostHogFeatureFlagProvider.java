package net.hollowcube.mapmaker.feature.posthog;

import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.metrics.MetricWriterPosthog;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.posthog.FeatureFlagContext;
import net.hollowcube.posthog.PostHog;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PostHogFeatureFlagProvider implements FeatureFlagProvider {
    private static final Logger log = LoggerFactory.getLogger(PostHogFeatureFlagProvider.class);

    // Uses the global PostHog instance which must have been initialized prior to this moment.

    @Override
    public boolean test(@NotNull String name, @NotNull Object... context) {
        var transformedName = name.replace(".", "_");
        if (context.length == 0)
            return PostHog.getFeatureFlag(transformedName, MetricWriterPosthog.NO_USER).isEnabled();
        return switch (context[0]) {
            case Player player -> {
                var playerData = PlayerDataV2.fromPlayer(player);
                yield PostHog.getFeatureFlag(transformedName, playerData.id(), FeatureFlagContext.newBuilder()
                        .personProperties(Map.of(
                                "username", playerData.username()
                                // TODO: we should include hypercube status, but we would need to cache that here.
                                // Would be nice to have playerData.hasHypercube() or something.
                                // Likely player service would inject the hypercube end date for this player
                                // and we would compare against now. It would also need to be updated when
                                // we get a new hypercube event.
                        ))
                        .build()).isEnabled();
            }
            case PlayerDataV2 playerData ->
                    PostHog.getFeatureFlag(transformedName, playerData.id(), FeatureFlagContext.newBuilder()
                            .personProperties(Map.of(
                                    "username", playerData.username()
                            ))
                            .build()).isEnabled();
            case MapData map -> {
                log.warn("Map context is not supported: {}", map);
                yield false;
            }
            default -> {
                log.warn("Unknown context: {}", context[0]);
                yield false;
            }
        };
    }
}
