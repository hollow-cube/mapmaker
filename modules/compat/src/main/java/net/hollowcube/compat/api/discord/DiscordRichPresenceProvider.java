package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;

public interface DiscordRichPresenceProvider {
    void setRichPresence(Player player, String playerState, String gameName, String gameVariantName);

    void clearRichPresence(Player player);

    boolean isRichPresenceSupportedFor(Player player);
}
