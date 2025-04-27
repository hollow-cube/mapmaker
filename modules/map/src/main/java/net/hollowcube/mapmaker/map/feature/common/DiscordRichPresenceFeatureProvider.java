package net.hollowcube.mapmaker.map.feature.common;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.WordUtil;
import net.hollowcube.compat.api.discord.DiscordRichPresenceManager;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class DiscordRichPresenceFeatureProvider implements FeatureProvider {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:map/discord-rich-presence", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handleMapInit);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof EditingMapWorld))
            return false;
        world.eventNode().addChild(eventNode);
        return true;
    }

    private void handleMapInit(@NotNull final MapPlayerInitEvent event) {
        if (event.isMapJoin()) {
            if (event.mapWorld() instanceof PlayingMapWorld) {
                DiscordRichPresenceManager.setRichPresence(event.player(), "Playing", event.mapWorld().map().name(), "/play " + event.mapWorld().map().publishedIdString());
            } else if (event.mapWorld() instanceof EditingMapWorld) {
                final var variant = event.mapWorld().map().settings().getVariant().name().toLowerCase();
                DiscordRichPresenceManager.setRichPresence(event.getPlayer(), "Building", "a map", "Building " + WordUtil.indefiniteArticle(variant) + " map");
            }
        }
    }

}

