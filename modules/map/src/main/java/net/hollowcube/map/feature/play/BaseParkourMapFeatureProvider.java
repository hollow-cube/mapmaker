package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.gui.hotbar.PlayingMapHotbar;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class BaseParkourMapFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/parkour", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        world.addScopedEventNode(eventNode);

        // Controls player visibility
        world.instance().scheduler()
                .buildTask(() -> updateViewership(world))
                .repeat(TaskSchedule.tick(5))
                .schedule();

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;
        if (!event.isFirstInit()) return;

        PlayingMapHotbar.applyToPlayer(event.mapWorld(), player);

        player.updateViewableRule(p -> {
            if (p.isInvisible()) return true;
            return player.getDistanceSquared(p) > 3.5 * 3.5;
        });
    }

    public void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        player.updateViewableRule((p) -> true);
    }

    private void updateViewership(@NotNull MapWorld world) {
        for (Player p : world.players()) {
            p.updateViewableRule();
        }
    }

}
