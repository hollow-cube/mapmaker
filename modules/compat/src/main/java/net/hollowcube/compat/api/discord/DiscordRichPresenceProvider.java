package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;

public interface DiscordRichPresenceProvider {
    void setRichPresence(
            final Player player,
            final String gameName,
            final String gameVariantName,
            final String playerState
    );

    void clearRichPresence(final Player player);

    boolean isRichPresenceSupportedFor(final Player player);
}
