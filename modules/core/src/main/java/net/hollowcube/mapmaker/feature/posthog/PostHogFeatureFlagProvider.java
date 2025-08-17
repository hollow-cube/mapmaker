package net.hollowcube.mapmaker.feature.posthog;

import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.posthog.FeatureFlagContext;
import net.hollowcube.posthog.PostHog;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PostHogFeatureFlagProvider implements FeatureFlagProvider {
    public static final String NO_USER = "00000000-0000-0000-0000-000000000000";

    private static final Logger log = LoggerFactory.getLogger(PostHogFeatureFlagProvider.class);

    // Uses the global PostHog instance which must have been initialized prior to this moment.

    @Override
    public boolean test(@NotNull String name, @NotNull Object... context) {
        var transformedName = name.replace(".", "_");
        if (context.length == 0)
            return PostHog.getFeatureFlag(transformedName, NO_USER).isEnabled();
        return switch (context[0]) {
            case Player player -> {
                var playerData = PlayerData.fromPlayer(player);
                yield PostHog.getFeatureFlag(transformedName, playerData.id(), FeatureFlagContext.newBuilder()
                        .personProperties(Map.of(
                                "username", playerData.username(),
                                "is_hypercube", playerData.isHypercube()
                        ))
                        .build()).isEnabled();
            }
            case PlayerData playerData ->
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
