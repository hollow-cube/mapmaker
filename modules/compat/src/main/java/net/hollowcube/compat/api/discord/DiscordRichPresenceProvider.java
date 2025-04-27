package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface DiscordRichPresenceProvider {
    void setRichPresence(@NotNull Player player, @NotNull String playerState, @NotNull String gameName, @NotNull String gameVariantName);

    void clearRichPresence(@NotNull Player player);

    boolean isRichPresenceSupportedFor(@NotNull Player player);
}
