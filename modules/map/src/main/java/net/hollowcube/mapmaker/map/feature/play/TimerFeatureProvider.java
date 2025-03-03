package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.misc.BackgroundSpriteSet;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

@AutoService(FeatureProvider.class)
public class TimerFeatureProvider implements FeatureProvider {

    private static final BackgroundSpriteSet BACKGROUND = new BackgroundSpriteSet("hud/bossbar/line1");
    private static final BadSprite TIMER = BadSprite.SPRITE_MAP.get("hud/timer");
    private static final int BACKGROUND_PADDING = 2;

    private final EventNode<InstanceEvent> eventNode = EventNode.type("hud/timer", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handleStartPlaying)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handleStopPlaying);

    private final ActionBar.Provider actionBar = this::buildWidget;

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR) return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    private void handleStartPlaying(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;
        ActionBar.forPlayer(event.player()).addProvider(actionBar);
    }

    private void handleStopPlaying(@NotNull MapWorldPlayerStopPlayingEvent event) {
        ActionBar.forPlayer(event.player()).removeProvider(actionBar);
    }

    private void buildWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return;

        long time = saveState.getRealPlaytime();
        var effects = world.getTag(BaseParkourMapFeatureProvider.SPAWN_CHECKPOINT_EFFECTS);

        // Append the countdown timer, but only if it's not a testing map.
        // We should not show the normal timer in testing mode.
        // If the countdown is not and it's the start of the map show the time limit.
        var countdownEnd = player.getTag(BaseParkourMapFeatureProvider.COUNTDOWN_END);
        if (countdownEnd != -1) {
            time = Math.max(countdownEnd - System.currentTimeMillis(), 0);
        } else if (time == 0 && effects != null && effects.timeLimit() > 0) {
            time = effects.timeLimit();
        } else if (world instanceof TestingMapWorld) {
            return;
        }

        var text = formatMapPlaytime(time, false);
        // Text + spacing of same size of the ends of the background + timer width
        var width = FontUtil.measureTextV2(text) + BACKGROUND_PADDING * 4 + TIMER.width();

        builder.pushShadowColor(ShadowColor.none());
        builder.pos(-width / 2);
        builder.append(BACKGROUND.build(width - BACKGROUND_PADDING * 2), width);
        builder.offset(-width);
        builder.offset(BACKGROUND_PADDING);
        builder.drawInPlace(TIMER);
        builder.offset(BACKGROUND_PADDING);
        builder.append("bossbar_ascii_1", text);
        builder.popShadowColor();
    }
}
