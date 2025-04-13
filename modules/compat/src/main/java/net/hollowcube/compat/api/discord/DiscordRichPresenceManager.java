package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;

import java.util.ServiceLoader;

public interface DiscordRichPresenceManager {

    void setRichPresence(Player player, String gameName, String gameVariantName, String playerState);

    static void load() {
        ServiceLoader.load(DiscordRichPresenceManager.class).findFirst().orElseThrow();
    }
}
