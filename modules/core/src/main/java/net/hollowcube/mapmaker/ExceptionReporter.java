package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.posthog.PostHog;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExceptionReporter {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionReporter.class);

    public static void reportException(Throwable t) {
        logger.error("Unhandled exception", t);
        PostHog.captureException(t);
    }

    public static void reportException(Throwable t, PlayerData playerData) {
        reportException(t, playerData.id());
    }

    public static void reportException(Throwable t, Player player) {
        reportException(t, player.getUuid().toString());
    }

    public static void reportException(Throwable t, String playerId) {
        logger.error("Unhandled exception for player {}", playerId, t);
        PostHog.captureException(t, playerId);
    }
}
