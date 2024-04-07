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
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
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
        player.addEffect(new Potion(PotionEffect.JUMP_BOOST, (byte) -8, Potion.INFINITE_DURATION));
    }

    private void removeEffect(@NotNull Player player) {
        player.removeEffect(PotionEffect.JUMP_BOOST);
    }
}
