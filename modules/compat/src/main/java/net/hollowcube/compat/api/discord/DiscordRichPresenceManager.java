package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;


public class DiscordRichPresenceManager {
    private static final Set<DiscordRichPresenceProvider> PROVIDERS = new HashSet<>();

    static {
        ServiceLoader.load(DiscordRichPresenceProvider.class).forEach(PROVIDERS::add);
    }


    public static void queueRichPresenceUpdate(@NotNull Player player, @NotNull String line1, @NotNull String line2) {
        player.scheduler().buildTask(() -> {
            for (DiscordRichPresenceProvider provider : PROVIDERS) {
                if (provider.isRichPresenceSupportedFor(player)) {
                    provider.setRichPresence(player, line1, line2);
                    break;
                }
            }
        }).delay(Duration.ofMillis(2500)).schedule();
    }

    public static void clearRichPresence(@NotNull Player player) {
        for (DiscordRichPresenceProvider provider : PROVIDERS) {
            if (provider.isRichPresenceSupportedFor(player)) {
                provider.clearRichPresence(player);
                return;
            }
        }
    }
}
