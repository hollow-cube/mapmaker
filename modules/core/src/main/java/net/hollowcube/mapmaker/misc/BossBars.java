package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BossBars {
    private static final BackgroundSpriteSet BG_LINE_1 = new BackgroundSpriteSet("hud/bossbar/line1");
    private static final BackgroundSpriteSet BG_LINE_2 = new BackgroundSpriteSet("hud/bossbar/line2");
    private static final int BORDER_WIDTH = 4;

    public static final BossBar ADDRESS_LINE = BossBars.createLine2(
            Component.text(FontUtil.rewrite("bossbar_small_2", "hollowcube.net"), TextColor.color(0xF2F2F2)));

    public static void clear(@NotNull Player player) {
        var bars = MinecraftServer.getBossBarManager().getPlayerBossBars(player);
        bars.forEach(b -> MinecraftServer.getBossBarManager().removeBossBar(player, b));
    }

    public static @NotNull BossBar createLine1(@NotNull Component text) {
        return build(text, BG_LINE_1);
    }

    public static @NotNull BossBar createLine2(@NotNull Component text) {
        return build(text, BG_LINE_2);
    }

    private static @NotNull BossBar build(@NotNull Component text, @NotNull BackgroundSpriteSet bg) {
        int contentWidth = FontUtil.measureTextV2(text);
        return bossBar(Component.text()
                .append(Component.text(bg.build(contentWidth + (2 * BORDER_WIDTH)), FontUtil.NO_SHADOW))
                .append(Component.text(FontUtil.computeOffset(-(contentWidth + BORDER_WIDTH + 2))))
                .append(text)
                .build());
    }

    private static @NotNull BossBar bossBar(@NotNull Component text) {
        return BossBar.bossBar(text, 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    }

}
