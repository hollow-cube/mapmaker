package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface DiscordRichPresenceProvider {
    void setRichPresence(@NotNull Player player, @NotNull String line1, @NotNull String line2);

    void clearRichPresence(@NotNull Player player);

    boolean isRichPresenceSupportedFor(@NotNull Player player);
}
