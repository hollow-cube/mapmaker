package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.hollowcube.mapmaker.to_be_refactored.BadSprite.require;

public class ExpBarRenderer implements PlayerHud.Module {
    private static final int XP_BAR_WIDTH = 182;
    private static final int BAR_Y = -36;
    private static final int NUMS_Y = -43;

    private static final BadSprite[] XP_BAR_BACKGROUND = new BadSprite[]{
            require("hud/level/xp_bar_background_off"), require("hud/level/xp_bar_background_on"),
    };
    private static final BadSprite[] XP_BAR_EDGE = new BadSprite[]{
            require("hud/level/xp_bar_edge_off"), require("hud/level/xp_bar_edge_on")
    };
    private static final BadSprite[] XP_BAR_SEGMENT = new BadSprite[]{
            require("hud/level/xp_bar_segment_off"), require("hud/level/xp_bar_segment_on")
    };
    private static final BadSprite[] XP_BAR_SEGMENT_EDGE = new BadSprite[]{
            require("hud/level/xp_bar_segment_edge_off"), require("hud/level/xp_bar_segment_edge_on")
    };
    private static final BadSprite[] XP_BAR_SEGMENT_DIVIDER = new BadSprite[]{
            require("hud/level/xp_bar_segment_divider_off"), require("hud/level/xp_bar_segment_divider_on")
    };
    private static final BadSprite[] XP_BAR_SEGMENT_CENTER = new BadSprite[]{
            require("hud/level/xp_bar_segment_center_off"), require("hud/level/xp_bar_segment_center_on")
    };

    private static final BadSprite[] NUMS = new BadSprite[]{
            require("hud/level/num_0"),
            require("hud/level/num_1"), require("hud/level/num_2"), require("hud/level/num_3"),
            require("hud/level/num_4"), require("hud/level/num_5"), require("hud/level/num_6"),
            require("hud/level/num_7"), require("hud/level/num_8"), require("hud/level/num_9")
    };

    private long lastExp = -1;
    private long lastBarKey = -1;
    private HudNode.Anchored lastBar = null; // late init

    @Override
    public @Nullable HudNode.Anchored render(@NotNull Player player) {
        // Update the player experience bar if it has changed
        var playerData = PlayerData.fromPlayer(player);
        if (playerData.experience() != lastExp) {
            player.setLevel(playerData.level());
            player.setExp(playerData.levelProgress());
            lastExp = playerData.experience();
        }

        // Only creative uses the custom bar: survival/adventure use the builtin one, and spectator
        // shows nothing (it generally makes no sense, but also Axiom uses spectator when in editor
        // mode, which should not show this ui for sure - it looks awful).
        if (player.getGameMode() != GameMode.CREATIVE) return null;

        int pixel = (int) (XP_BAR_WIDTH * player.getExp());
        int level = player.getLevel();

        // The bar assembly below is not cheap, so memoize on the visible state.
        long barKey = (long) pixel << 32 | level;
        if (lastBar != null && barKey == lastBarKey) return lastBar;

        var builder = new FontUIBuilder();
        builder.pushColor(HudText.COLOR_MARKER);
        builder.pushShadowColor(HudText.buildAnchorShadowMarker(HudAnchor.BOTTOM, BAR_Y, NamedTextColor.WHITE));

        builder.pos(-(XP_BAR_WIDTH / 2));
        if (pixel < 1) {
            // No exp, just speed up by drawing an empty background
            builder.drawInPlace(XP_BAR_BACKGROUND[0]);
        } else if (pixel >= XP_BAR_WIDTH) {
            // Full exp, just speed up by drawing a full background
            builder.drawInPlace(XP_BAR_BACKGROUND[1]);
        } else {
            int current = 1;
            builder.offset(1).drawInPlace(XP_BAR_EDGE[1]);

            while (current < pixel - 9) {
                builder.drawInPlace(XP_BAR_SEGMENT[1]);
                current += 9;

                if (current < pixel) {
                    builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[1]);
                    current++;
                }

                if (current < pixel && current == (XP_BAR_WIDTH / 2)) {
                    builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[1]);
                    current++;
                }
            }

            // Render the remaining part of the segment
            int remaining = pixel - current;
            if (current < pixel) {
                builder.drawInPlace(XP_BAR_SEGMENT_EDGE[1]);
                current++;
            }
            while (current <= pixel - (remaining == 9 ? 2 : 1)) {
                builder.drawInPlace(XP_BAR_SEGMENT_CENTER[1]);
                current++;
            }
            if (remaining == 9) {
                builder.drawInPlace(XP_BAR_SEGMENT_EDGE[1]);
                current++;
            }

            // Render the remaining part of the segment
            if (current == (XP_BAR_WIDTH / 2) || current == (XP_BAR_WIDTH / 2) - 1) {
                builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[0]);
                current++;
            }
            if (remaining == 0) {
                builder.drawInPlace(XP_BAR_SEGMENT_EDGE[0]);
                current++;
                remaining++;
            }
            for (int i = 0; i < 9 - remaining - 1; i++) {
                builder.drawInPlace(XP_BAR_SEGMENT_CENTER[0]);
                current++;
            }
            if (remaining <= 8) {
                builder.drawInPlace(XP_BAR_SEGMENT_EDGE[0]);
                current++;
            }
            if (current < XP_BAR_WIDTH - 1) {
                builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[0]);
                current++;
            }
            if (current == (XP_BAR_WIDTH / 2)) {
                builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[0]);
                current++;
            }

            while (current < XP_BAR_WIDTH - 9) {
                builder.drawInPlace(XP_BAR_SEGMENT[0]);
                current += 9;

                if (current < XP_BAR_WIDTH - 1) {
                    builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[0]);
                    current++;
                }

                if (current == (XP_BAR_WIDTH / 2)) {
                    builder.drawInPlace(XP_BAR_SEGMENT_DIVIDER[0]);
                    current++;
                }
            }

            builder.drawInPlace(XP_BAR_EDGE[0]);
        }

        builder.popShadowColor();

        if (level > 0) {
            builder.pushShadowColor(HudText.buildAnchorShadowMarker(HudAnchor.BOTTOM, NUMS_Y, NamedTextColor.WHITE));
            var levelChars = String.valueOf(level).toCharArray();
            builder.pos(-(6 * levelChars.length) / 2 + 1);
            for (char c : levelChars) {
                var sprite = NUMS[c - '0'];
                builder.offset(-1).drawInPlace(sprite);
            }
            builder.popShadowColor();
        }

        builder.popColor();

        lastBarKey = barKey;
        return lastBar = HudNode.raw(builder.build(true))
            .anchored(HudAnchor.BOTTOM);
    }

}
