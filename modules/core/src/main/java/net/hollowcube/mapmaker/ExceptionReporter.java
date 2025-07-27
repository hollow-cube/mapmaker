package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.posthog.PostHog;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExceptionReporter {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionReporter.class);

    public static void reportException(@NotNull Throwable t) {
        logger.error("Unhandled exception", t);
        PostHog.captureException(t);
    }

    public static void reportException(@NotNull Throwable t, @NotNull PlayerData playerData) {
        reportException(t, playerData.id());
    }

    public static void reportException(@NotNull Throwable t, @NotNull Player player) {
        reportException(t, player.getUuid().toString());
    }

    public static void reportException(@NotNull Throwable t, @NotNull String playerId) {
        logger.error("Unhandled exception for player {}", playerId, t);
        PostHog.captureException(t, playerId);
    }
}
