package net.hollowcube.common.hud;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudBakerTest {

    private record Run(String content, ShadowColor shadowColor) {
    }

    @Test
    void netZeroAdvance() {
        var baked = HudBaker.bake(List.of(
                HudNode.vstack(1, HudNode.Align.LEFT,
                                HudNode.text("hello"), HudNode.text("world!"))
                        .offset(130, -35).anchored(HudAnchor.BOTTOM),
                HudNode.text("abc").frame(22, HudNode.Align.RIGHT).offset(4, -3).offset(10, 5)
                        .anchored(HudAnchor.TOP_LEFT)));
        assertEquals(0, measure(flatten(baked)));
    }

    @Test
    void frameAlignsRight() {
        var baked = HudBaker.bake(List.of(
                HudNode.text("ab").frame(22, HudNode.Align.RIGHT).anchored(HudAnchor.BOTTOM)));
        assertEquals(22 - FontUtil.measureText("ab"), posOf(flatten(baked), "ab"));
    }

    @Test
    void frameZeroCenterCentersOnOrigin() {
        var baked = HudBaker.bake(List.of(
                HudNode.text("abcd").frame(0, HudNode.Align.CENTER).anchored(HudAnchor.BOTTOM)));
        assertEquals(-FontUtil.measureText("abcd") / 2, posOf(flatten(baked), "abcd"));
    }

    @Test
    void vstackRowsCarryTheirOwnMarkers() {
        var baked = HudBaker.bake(List.of(
                HudNode.vstack(1, HudNode.Align.LEFT, HudNode.text("one"), HudNode.text("two"))
                        .offset(130, -35).anchored(HudAnchor.BOTTOM)));
        var runs = flatten(baked);
        assertEquals(130, posOf(runs, "one"));
        assertEquals(130, posOf(runs, "two"));
        assertEquals(HudText.buildAnchorShadowMarker(HudAnchor.BOTTOM, -35, WHITE), shadowOf(runs, "one"));
        // 9px text row + 1px gap
        assertEquals(HudText.buildAnchorShadowMarker(HudAnchor.BOTTOM, -25, WHITE), shadowOf(runs, "two"));
    }

    @Test
    void offsetIsRenderOnly() {
        var baked = HudBaker.bake(List.of(
                HudNode.hstack(HudNode.text("ab").offset(5, 0), HudNode.text("cd"))
                        .anchored(HudAnchor.BOTTOM)));
        var runs = flatten(baked);
        assertEquals(5, posOf(runs, "ab"));
        // The sibling's flow position ignores its render offset.
        assertEquals(FontUtil.measureText("ab"), posOf(runs, "cd"));
    }

    @Test
    void shadowDrawsDarkenedDisplacedCopyFirst() {
        var baked = HudBaker.bake(List.of(
            HudNode.text("ab").shadow().offset(4, 10).anchored(HudAnchor.TOP_LEFT)));
        var runs = flatten(baked);

        int x = 0;
        var positions = new ArrayList<Integer>();
        var markers = new ArrayList<ShadowColor>();
        for (var run : runs) {
            if (run.content().equals("ab")) {
                positions.add(x);
                markers.add(run.shadowColor());
            }
            x += FontUtil.measureText(run.content());
        }
        assertEquals(0, x); // net-zero even with the doubled content

        // Shadow copy first (painted under), +1,+1 displaced, tint quartered; real copy on top.
        assertEquals(List.of(5, 4), positions);
        assertEquals(HudText.buildAnchorShadowMarker(HudAnchor.TOP_LEFT, 11, TextColor.color(63, 63, 63)), markers.get(0));
        assertEquals(HudText.buildAnchorShadowMarker(HudAnchor.TOP_LEFT, 10, WHITE), markers.get(1));
    }

    @Test
    void zstackOverlaps() {
        var baked = HudBaker.bake(List.of(
                HudNode.zstack(HudNode.text("under"), HudNode.text("over"))
                        .offset(7, 0).anchored(HudAnchor.CENTER)));
        var runs = flatten(baked);
        assertEquals(7, posOf(runs, "under"));
        assertEquals(7, posOf(runs, "over"));
    }

    private static List<Run> flatten(Component component) {
        var runs = new ArrayList<Run>();
        collect(component, runs);
        return runs;
    }

    private static void collect(Component component, List<Run> runs) {
        if (component instanceof TextComponent text && !text.content().isEmpty())
            runs.add(new Run(text.content(), text.shadowColor()));
        for (var child : component.children()) collect(child, runs);
    }

    private static int measure(List<Run> runs) {
        int width = 0;
        for (var run : runs) width += FontUtil.measureText(run.content());
        return width;
    }

    private static int posOf(List<Run> runs, String content) {
        int x = 0;
        for (var run : runs) {
            if (run.content().equals(content)) return x;
            x += FontUtil.measureText(run.content());
        }
        throw new AssertionError("no run with content: " + content);
    }

    private static ShadowColor shadowOf(List<Run> runs, String content) {
        for (var run : runs) {
            if (run.content().equals(content)) return run.shadowColor();
        }
        throw new AssertionError("no run with content: " + content);
    }
}
