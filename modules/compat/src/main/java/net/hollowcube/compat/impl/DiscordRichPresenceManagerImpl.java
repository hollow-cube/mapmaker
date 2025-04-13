package net.hollowcube.compat.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.discord.DiscordRichPresenceManager;
import net.hollowcube.compat.api.discord.DiscordRichPresenceProvider;
import net.minestom.server.entity.Player;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

@AutoService(DiscordRichPresenceManager.class)
public class DiscordRichPresenceManagerImpl implements DiscordRichPresenceManager {
    private static final Set<DiscordRichPresenceProvider> PROVIDERS = new HashSet<>();

    private static DiscordRichPresenceManagerImpl INSTANCE;

    public DiscordRichPresenceManagerImpl() {
        INSTANCE = this;
        ServiceLoader.load(DiscordRichPresenceProvider.class).forEach(PROVIDERS::add);
    }

    public static DiscordRichPresenceManager getInstance() {
        if (INSTANCE != null) return INSTANCE;
        throw new IllegalStateException("DiscordRichPresenceManagerImpl not initialized");
    }


    @Override
    public void setRichPresence(final Player player, final String gameName, final String gameVariantName, final String playerState) {
        for (DiscordRichPresenceProvider provider : PROVIDERS) {
            System.out.println(provider.getClass().getName());
            if (provider.isRichPresenceSupportedFor(player)) {
                provider.setRichPresence(player, gameName, gameVariantName, playerState);
                return;
            }
        }
    }
}
