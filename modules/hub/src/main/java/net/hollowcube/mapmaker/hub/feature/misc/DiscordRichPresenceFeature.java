package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.discord.DiscordRichPresenceManager;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.minestom.server.event.player.PlayerLoadedEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class DiscordRichPresenceFeature implements HubFeature {
    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        world.eventNode().addListener(PlayerLoadedEvent.class, event -> DiscordRichPresenceManager.setRichPresence(event.getPlayer(), "In", "the lobby", ""));
    }
}
