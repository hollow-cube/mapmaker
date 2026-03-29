package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface DiscordRichPresenceProvider {
    void setRichPresence(@NotNull Player player, @NotNull String activity, @NotNull String map);

    void clearRichPresence(@NotNull Player player);

    boolean isRichPresenceSupportedFor(@NotNull Player player);
}
