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
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

@AutoService(FeatureProvider.class)
public class TimerFeatureProvider implements FeatureProvider {
    private static final BadSprite TIMER_CONTAINER = BadSprite.SPRITE_MAP.get("hud/timer_container");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("hud/timer", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handleStartPlaying)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handleStopPlaying)
            .addListener(PlayerMoveEvent.class, this::handlePlayerMove);

    private final ActionBar.Provider actionBar = this::buildWidget;

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
        ActionBar.forPlayer(event.player()).addProvider(actionBar);
    }

    private void handleStopPlaying(@NotNull MapWorldPlayerStopPlayingEvent event) {
        ActionBar.forPlayer(event.player()).removeProvider(actionBar);
    }

    private void handlePlayerMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || saveState.getPlayStartTime() != 0) return;

        var oldPosition = player.getPosition();
        var newPosition = event.getNewPosition();
        if (Vec.fromPoint(oldPosition).equals(Vec.fromPoint(newPosition)))
            return; // Player did not actually move, just turn their head

        saveState.setPlayStartTime(System.currentTimeMillis());
    }

    private void buildWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        if (!MapHooks.isPlayerPlaying(player)) return;

        var saveState = SaveState.fromPlayer(player);

        long time = 0;
        if (saveState.getPlayStartTime() != 0) {
            time = saveState.getPlaytime() + System.currentTimeMillis() - saveState.getPlayStartTime();
        }

        var text = formatMapPlaytime(time, false);

        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(-TIMER_CONTAINER.width() / 2).drawInPlace(TIMER_CONTAINER);
        builder.pos(-TIMER_CONTAINER.width() / 2 + 19).append(text);
    }
}
