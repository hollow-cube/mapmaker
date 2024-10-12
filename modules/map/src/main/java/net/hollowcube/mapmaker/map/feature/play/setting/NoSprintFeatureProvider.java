package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapSpectatorToggleFlightEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoSprintFeatureProvider implements FeatureProvider {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/nosprint", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer)
            .addListener(MapSpectatorToggleFlightEvent.class, this::handleSpectatorFlightToggle);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isNoSprint())
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        applyEffect(player);

        if (event.isMapJoin()) {
            player.sendMessage(Component.translatable("map.join.warning.setting.no_sprint"));
        }
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        removeEffect(event.getPlayer());
    }

    public void handleSpectatorFlightToggle(@NotNull MapSpectatorToggleFlightEvent event) {
        var player = event.player();
        if (event.newState()) {
            applyEffect(player);
        } else {
            removeEffect(player);
        }
    }

    private void applyEffect(@NotNull Player player) {
        player.setFood(6);
    }

    private void removeEffect(@NotNull Player player) {
        player.setFood(20);
    }
}
