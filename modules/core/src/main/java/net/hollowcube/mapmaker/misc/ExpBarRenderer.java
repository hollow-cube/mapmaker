package net.hollowcube.mapmaker.misc;

import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.to_be_refactored.BadSprite.require;

public class ExpBarRenderer implements ActionBar.Provider {
    private static final int XP_BAR_WIDTH = 182;

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

    public ExpBarRenderer() {

    }

    private long lastExp = -1;

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        // Never show in spectator. It generally makes no sense, but also Axiom uses spectator when in editor mode,
        // which should not show this ui for sure (it looks awful).
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        // Update the player experience bar if it has changed
        var playerData = PlayerDataV2.fromPlayer(player);
        if (playerData.experience() != lastExp) {
            player.setLevel(playerData.level());
            player.setExp(playerData.levelProgress());
            lastExp = playerData.experience();
        }

        var hasExperienceBar = player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
        if (hasExperienceBar) return; // Use the builtin one for these.

        builder.pushShadowColor(ShadowColor.none());

        int pixel = (int) (XP_BAR_WIDTH * player.getExp());

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

        int level = player.getLevel();
        if (level > 0) {
            var levelChars = String.valueOf(level).toCharArray();
            builder.pos(-(6 * levelChars.length) / 2 + 1);
            for (char c : levelChars) {
                var sprite = NUMS[c - '0'];
                builder.offset(-1).drawInPlace(sprite);
            }
        }

        builder.popColor();
    }

}
