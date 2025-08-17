package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoRelogFeatureProvider extends AbstractSettingFeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/norelog", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static boolean canRelog(@NotNull Player player, @NotNull MapWorld world) {
        if (!world.isPlaying(player)) return true;

        var state = SaveState.optionalFromPlayer(player);
        if (state == null) return true; // Sanity
        var playstate = state.state(PlayState.class);
        return !playstate.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_RELOG, world.map().settings());
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (canRelog(player, event.getMapWorld())) return;
        if (!event.isMapJoin()) return;

        player.sendMessage(Component.translatable("map.join.warning.setting.no_relog"));

        EventDispatcher.call(new MapPlayerResetEvent(player, event.getMapWorld(), true));
    }
}
