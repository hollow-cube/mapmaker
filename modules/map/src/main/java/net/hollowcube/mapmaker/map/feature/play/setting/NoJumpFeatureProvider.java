package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapSpectatorToggleFlightEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoJumpFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/nojump", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer)
            .addListener(MapSpectatorToggleFlightEvent.class, this::handleSpectatorFlightToggle)
            .addListener(MapPlayerCheckpointChangeEvent.class, this::handleCheckpointChange);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isNoJump())
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        addEffect(player);

        if (event.isMapJoin()) {
            player.sendMessage(Component.translatable("map.join.warning.setting.no_jump"));
        }
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        removeEffect(event.getPlayer());
    }

    public void handleSpectatorFlightToggle(@NotNull MapSpectatorToggleFlightEvent event) {
        var player = event.player();
        if (event.newState()) {
            addEffect(player);
        } else {
            removeEffect(player);
        }
    }

    public void handleCheckpointChange(@NotNull MapPlayerCheckpointChangeEvent event) {
        addEffect(event.getPlayer());
    }

    private void addEffect(@NotNull Player player) {
        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0);
    }

    private void removeEffect(@NotNull Player player) {
        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(Attribute.GENERIC_JUMP_STRENGTH.defaultValue());
    }
}
