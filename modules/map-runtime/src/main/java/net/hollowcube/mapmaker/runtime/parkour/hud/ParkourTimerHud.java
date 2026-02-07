package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.misc.BackgroundSpriteSet;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditTimerAction;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

public class ParkourTimerHud implements ActionBar.Provider {
    private static final BackgroundSpriteSet BACKGROUND = new BackgroundSpriteSet("hud/bossbar/line1");
    private static final BadSprite TIMER = BadSprite.SPRITE_MAP.get("hud/timer");
    private static final int BACKGROUND_PADDING = 2;

    public static final ParkourTimerHud INSTANCE = new ParkourTimerHud();

    private ParkourTimerHud() {
    }

    @Override
    public int cacheKey(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        var state = world == null ? null : world.getPlayerState(player);
        var saveState = state instanceof ParkourState.AnyPlaying p ? p.saveState() : null;
        return saveState == null ? -1 : Long.hashCode(saveState.getRealPlaytime());
    }

    @Override
    public void provide(Player player, FontUIBuilder builder) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return;

        long time = p.saveState().getRealPlaytime();
        var startingTimer = OpUtils.map(world.getTag(ParkourMapWorld.SPAWN_CHECKPOINT_EFFECTS),
                checkpoint -> checkpoint.actions().findLast(EditTimerAction.class));

        // Append the countdown timer, but only if it's not a testing map.
        // We should not show the normal timer in testing mode.
        // If the countdown is not and it's the start of the map show the time limit.
        var countdownEnd = player.getTag(EditTimerAction.COUNTDOWN_END);
        if (countdownEnd != -1) {
            time = Math.max(countdownEnd - System.nanoTime() / 1_000_000, 0);
        } else if (time == 0 && startingTimer != null && startingTimer.time() > 0) {
            time = startingTimer.time() * 50L; // Ticks to milliseconds
        } else if (!(p instanceof ParkourState.Playing2)) {
            return; // Don't show the normal timer for testing, only playing
        }

        var text = formatMapPlaytime(time, true);
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ParkourTimerHud;
    }

    @Override
    public int hashCode() {
        return 31 * ParkourTimerHud.class.hashCode();
    }
}
