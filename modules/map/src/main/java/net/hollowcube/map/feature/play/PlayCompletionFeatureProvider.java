package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.util.FireworkUtil;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.map.feature.play.TimerFeatureProvider.formatPlaytime;

@AutoService(FeatureProvider.class)
public class PlayCompletionFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enable this feature if the world is playing _and not testing_
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0 || (world.flags() & MapWorld.FLAG_TESTING) != 0)
            return false;

        var eventNode = EventNode.type("map-completion/play", EventFilter.INSTANCE);
        eventNode.addListener(MapWorldCompleteEvent.class, FutureUtil.virtual(this::handleMapCompletion));
        world.addScopedEventNode(eventNode);

        return true;
    }

    private @Blocking void handleMapCompletion(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        var world = (InternalMapWorld) MapWorld.forPlayer(player);

        var saveState = SaveState.fromPlayer(player);
        saveState.setCompleted(true);

        var bestSaveState = world.server().mapService().getBestSaveState(world.map().id(), player.getUuid().toString());

        // Remove the player from the world itself, they are no longer playing (but will remain in the instance)
        // This will also cause their savestate to be written to DB
        world.removePlayer(player);
        if (world instanceof PlayingMapWorld pmw) {
            pmw.startSpectating(player, false);
        }

        if (bestSaveState == null) {
            player.sendMessage(Component.translatable("map.completed.first", Component.text(formatPlaytime(saveState.getPlaytime()))));
        } else {
            var diffPlaytime = bestSaveState.getPlaytime() - saveState.getPlaytime();
            player.sendMessage(Component.translatable("map.completed.with_prior",
                    Component.text(formatPlaytime(saveState.getPlaytime())),
                    Component.text((diffPlaytime < 0 ? "+" : "-") + formatPlaytime(Math.abs(diffPlaytime)), diffPlaytime < 0 ? NamedTextColor.RED : NamedTextColor.GREEN)));
        }

        FireworkUtil.showFirework(event.getPlayer(), event.getInstance(), event.getPlayer().getPosition(), 15, List.of(FireworkUtil.randomColorEffect()));

    }

}
