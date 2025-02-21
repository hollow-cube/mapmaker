package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoSprintFeatureProvider extends AbstractSettingFeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/nosprint", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerUpdateStateEvent.class, this::playerUpdated)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static boolean canSprint(@NotNull Player player, MapWorld world) {
        if (!world.isPlaying(player)) return true;

        var state = SaveState.fromPlayer(player);
        var playstate = state.state(PlayState.class);
        return !playstate.settings().get(MapSettings.NO_SPRINT, world.map().settings());
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.player();
        if (!event.mapWorld().isPlaying(player)) return;
        if (canSprint(player, event.mapWorld())) return;
        if (!event.isMapJoin()) return;

        player.sendMessage(Component.translatable("map.join.warning.setting.no_sprint"));
    }

    public void playerUpdated(@NotNull MapPlayerUpdateStateEvent event) {
        updatePlayer(event.player());
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        event.player().setFood(20);
    }

    private void updatePlayer(@NotNull Player player) {
        var world = MapWorld.forPlayer(player);
        var canSprint = canSprint(player, world);
        player.setFood(canSprint ? 20 : 6);
    }
}
