package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;


public class DiscordRichPresenceManager {
    private static final Set<DiscordRichPresenceProvider> PROVIDERS = new HashSet<>();

    static {
        ServiceLoader.load(DiscordRichPresenceProvider.class).forEach(PROVIDERS::add);
    }


    public static void setRichPresence(Player player, String playerState, String gameName, String gameVariantName) {
        for (DiscordRichPresenceProvider provider : PROVIDERS) {
            if (provider.isRichPresenceSupportedFor(player)) {
                provider.setRichPresence(player, playerState, gameName, gameVariantName);
                return;
            }
        }
    }

    public static void clearRichPresence(Player player) {
        for (DiscordRichPresenceProvider provider : PROVIDERS) {
            if (provider.isRichPresenceSupportedFor(player)) {
                provider.clearRichPresence(player);
                return;
            }
        }
    }
}
