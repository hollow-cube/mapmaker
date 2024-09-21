package net.hollowcube.mapmaker.feature.posthog;

import com.posthog.java.PostHog;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.FlagContext;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.metrics.MetricWriterPosthog;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostHogFeatureFlagProvider implements FeatureFlagProvider {
    private static final Logger log = LoggerFactory.getLogger(PostHogFeatureFlagProvider.class);

    private final PostHog client;
    private final boolean defaultAction;

    public PostHogFeatureFlagProvider() {
        this.client = MetricWriterPosthog.POSTHOG_CLIENT;
        this.defaultAction = false;
    }

    @Override
    public boolean test(@NotNull String name, @NotNull Object... context) {
        if (client == null) return defaultAction;
        var transformedName = name.replace(".", "_");
        if (context.length == 0)
            return client.isFeatureFlagEnabled(transformedName, MetricWriterPosthog.NO_USER);
        return switch (context[0]) {
            case FlagContext flag -> {
                if (flag.name().equals("userId")) {
                    yield client.isFeatureFlagEnabled(transformedName, flag.value());
                } else {
                    log.warn("Unknown flag context: {}", flag);
                    yield false;
                }
            }
            case Player player -> client.isFeatureFlagEnabled(transformedName, PlayerDataV2.fromPlayer(player).id());
            case PlayerDataV2 pd -> client.isFeatureFlagEnabled(transformedName, pd.id());
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
