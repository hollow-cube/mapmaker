package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.List;

public class BossBars {
    private static final BackgroundSpriteSet BG_LINE_1 = new BackgroundSpriteSet("hud/bossbar/line1");
    private static final BackgroundSpriteSet BG_LINE_2 = new BackgroundSpriteSet("hud/bossbar/line2");
    private static final int BORDER_WIDTH = 4;

    public static final BossBar ADDRESS_LINE = BossBars.createLine2(
        Component.text(FontUtil.rewrite("bossbar_small_2", "hollowcube.net"), NamedTextColor.WHITE));

    public static void clear(Player player) {
        var bars = MinecraftServer.getBossBarManager().getPlayerBossBars(player);
        bars.forEach(b -> MinecraftServer.getBossBarManager().removeBossBar(player, b));
    }

    public static BossBar createLine1(Component text) {
        return build(text, BG_LINE_1);
    }

    public static BossBar createLine2(Component text) {
        return build(text, BG_LINE_2);
    }

    private static BossBar build(Component text, BackgroundSpriteSet bg) {
        int contentWidth = FontUtil.measureTextV2(text);
        return bossBar(
            Component.text()
                .append(Component.text(bg.build(contentWidth + (2 * BORDER_WIDTH))).shadowColor(ShadowColor.none()))
                .append(Component.text(FontUtil.computeOffset(-(contentWidth + BORDER_WIDTH + 2))))
                .append(text)
                .build()
        );
    }

    private static BossBar bossBar(Component text) {
        return BossBar.bossBar(text, 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    }

    // Helpers for common boss bar configurations

    public static List<BossBar> createPlayingBossBar(PlayerService playerService, MapData map) {
        Component ownerBossBarName;
        try {
            ownerBossBarName = playerService.getPlayerDisplayName2(map.owner())
                .build(DisplayName.Context.BOSS_BAR);
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
            ownerBossBarName = Component.text("!error!", NamedTextColor.RED);
        }

        return List.of(
            BossBars.createLine1(map.verification() == MapVerification.PENDING
                        ? Component.text()
                        .append(Component.text(FontUtil.rewrite("bossbar_small_1", "verifying") + " ", NamedTextColor.WHITE))
                        .append(Component.text(FontUtil.rewrite("bossbar_ascii_1", map.name()), TextColor.color(0xF2F2F2))).build()
                        : Component.text()
                        .append(Component.text(FontUtil.rewrite("bossbar_small_1", "playing") + " ", NamedTextColor.WHITE))
                        .append(MapData.rewriteWithQualityFont(map.quality(), FontUtil.rewrite("bossbar_ascii_1", map.name())))
                        .append(Component.text(" " + FontUtil.rewrite("bossbar_small_1", "by") + " ", TextColor.color(0xB0B0B0)))
                        .append(ownerBossBarName).build()),
                !map.isPublished() ? BossBars.ADDRESS_LINE
                        : BossBars.createLine2(Component.text()
                        .append(Component.text(FontUtil.rewrite("bossbar_small_2", "/play"), NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text(FontUtil.rewrite("bossbar_small_2", map.publishedIdString()), NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text(FontUtil.rewrite("bossbar_small_2", "on"), TextColor.color(0xB0B0B0)))
                        .appendSpace()
                        .append(Component.text(FontUtil.rewrite("bossbar_small_2", "hollowcube.net"), NamedTextColor.WHITE))
                        .build())
        );
    }

    public static List<BossBar> createEditingBossBar(MapData map) {
        var builder = Component.text()
            .append(Component.text(FontUtil.rewrite("bossbar_small_1", "building") + " ", NamedTextColor.WHITE))
            .append(Component.text(FontUtil.rewrite("bossbar_ascii_1", map.name()), TextColor.color(0x30FBFF)));

//        final String permissionName = player.getUuid().toString().equals(map().owner()) ? "Owner" : "Builder";
//        builder.append(Component.text("  " + FontUtil.rewrite("bossbar_small_1", "permission") + " ", TextColor.color(0xB0B0B0)))
//                .append(Component.text(FontUtil.rewrite("bossbar_ascii_1", permissionName), NamedTextColor.WHITE));

        return List.of(
            BossBars.createLine1(builder.build()),
            BossBars.ADDRESS_LINE
        );
    }
}
