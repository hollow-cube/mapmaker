package net.hollowcube.common.hud;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Diffs the declarative parkour timer tree against the old imperative FontUIBuilder emission
/// (the layout that was visually verified in game). Positions are computed with the same width
/// model the client font uses (verified against the generated pack's glyph advances).
class HBackgroundParityTest {
    private static final BackgroundSpriteSet BACKGROUND = new BackgroundSpriteSet("hud/bossbar/line1");
    private static final BadSprite TIMER = BadSprite.require("hud/timer");
    private static final int PADDING = 2;
    private static final String TEXT = "00:16.050";

    @Test
    void declarativeTimerMatchesOldImperativeLayout() {
        // The pre-declarative ParkourTimerHud emission, verbatim.
        var width = FontUtil.measureText(TEXT) + PADDING * 4 + TIMER.width();
        var builder = new FontUIBuilder();
        builder.pushColor(HudText.COLOR_MARKER);
        builder.pushShadowColor(HudText.buildAnchorShadowMarker(HudAnchor.BOTTOM, -72, WHITE));
        builder.pos(-width / 2);
        builder.append(BACKGROUND.build(width - PADDING * 2), width);
        builder.offset(-width);
        builder.offset(PADDING);
        builder.drawInPlace(TIMER);
        builder.offset(PADDING);
        builder.popShadowColor();
        builder.pushShadowColor(HudText.buildAnchorShadowMarker(HudAnchor.BOTTOM, -70, WHITE));
        builder.append(TEXT);
        builder.popShadowColor();
        builder.popColor();
        var old = builder.build(true);

        // The current ParkourTimerHud tree.
        var baked = HudBaker.bake(List.of(
            HudNode.hstack(PADDING + 1,
                    HudNode.sprite(TIMER),
                    HudNode.text(TEXT).offset(0, 2))
                .background(BACKGROUND, PADDING)
                .frame(0, HudNode.Align.CENTER)
                .offset(0, -72)
                .anchored(HudAnchor.BOTTOM)));

        char bgLeft = BadSprite.require("hud/bossbar/line1/left").fontChar();

        int oldBg = posOfChar(old, bgLeft), newBg = posOfChar(baked, bgLeft);
        int oldSprite = posOfChar(old, TIMER.fontChar()), newSprite = posOfChar(baked, TIMER.fontChar());
        int oldText = posOfText(old, TEXT), newText = posOfText(baked, TEXT);

        // The old code believed the bg advance was `width` when it was really `width + 1`, so it
        // was net +1; the baker must be exactly net-zero so modules cannot shift each other.
        assertEquals(1, advance(old, Integer.MAX_VALUE));
        assertEquals(0, advance(baked, Integer.MAX_VALUE));

        // The declarative layout may drift by a couple pixels (declared box vs forced advance),
        // but anything larger is a real regression.
        assertTrue(Math.abs(newBg - oldBg) <= 2, "bg: old=" + oldBg + " new=" + newBg);
        assertTrue(Math.abs(newSprite - oldSprite) <= 2, "sprite: old=" + oldSprite + " new=" + newSprite);
        assertTrue(Math.abs(newText - oldText) <= 2, "text: old=" + oldText + " new=" + newText);
    }

    // Position helpers: accumulate real glyph advances over the flattened component in order.

    private static List<String> flatten(Component component) {
        var runs = new ArrayList<String>();
        collect(component, runs);
        return runs;
    }

    private static void collect(Component component, List<String> runs) {
        if (component instanceof TextComponent text && !text.content().isEmpty()) runs.add(text.content());
        for (var child : component.children()) collect(child, runs);
    }

    private static int advance(Component component, int untilChar) {
        int x = 0;
        for (var run : flatten(component)) {
            for (int i = 0; i < run.length(); i++) {
                if (run.charAt(i) == untilChar) return x;
                x += FontUtil.measureText(String.valueOf(run.charAt(i)));
            }
        }
        if (untilChar != Integer.MAX_VALUE) throw new AssertionError("char not found: " + untilChar);
        return x;
    }

    private static int posOfChar(Component component, char c) {
        return advance(component, c);
    }

    private static int posOfText(Component component, String text) {
        int x = 0;
        for (var run : flatten(component)) {
            if (run.startsWith(text)) return x;
            x += FontUtil.measureText(run);
        }
        throw new AssertionError("text not found: " + text);
    }
}
