package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@AutoService(FeatureProvider.class)
public class TimerFeatureProvider implements FeatureProvider {
    private static final BadSprite TIMER_CONTAINER = BadSprite.SPRITE_MAP.get("hud/timer_container");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("hud/timer", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handleStartPlaying)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handleStopPlaying);

    private final ActionBar.Provider actionBar = (player, builder) -> {

    };

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0 || (world.flags() & MapWorld.FLAG_TESTING) != 0)
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR) return false;

        world.addScopedEventNode(eventNode);

        return true;
    }

    private void handleStartPlaying(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;
        ActionBar.forPlayer(event.player()).addProvider(this::buildWidget);
    }

    private void handleStopPlaying(@NotNull MapWorldPlayerStopPlayingEvent event) {
        ActionBar.forPlayer(event.player()).removeProvider(this::buildWidget);
    }

    private void buildWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        if (!MapHooks.isPlayerPlaying(player)) return;

        var saveState = SaveState.fromPlayer(player);
        var time = saveState.getPlaytime() + System.currentTimeMillis() - saveState.getPlayStartTime();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time));
        long milliseconds = time - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds);

        var text = String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);

        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(-TIMER_CONTAINER.width() / 2).drawInPlace(TIMER_CONTAINER);
        builder.pos(-TIMER_CONTAINER.width() / 2 + 19).append(text);
    }

    private void sendTimerActionBar(@NotNull MapWorld world) {
        for (var player : world.players()) {
            var text = new StringBuilder();
            text.append(TIMER_CONTAINER.fontChar());
            text.append(FontUtil.computeOffset(-52));

            var saveState = SaveState.fromPlayer(player);
            var time = saveState.getPlaytime() + System.currentTimeMillis() - saveState.getPlayStartTime();

            long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time));
            long milliseconds = time - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds);

            var a = String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
            text.append(a);

            player.sendActionBar(Component.text(text.toString(), TextColor.color(78, 92, 36)));
        }
    }
}
