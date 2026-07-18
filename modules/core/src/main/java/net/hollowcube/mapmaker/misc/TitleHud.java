package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudBar;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.player.DisplayName;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * The server title lines at the top of the screen (previously two boss bars), now anchored
 * hud modules. Line rows and per-charset offsets replicate the old layout: boss bar text
 * rows at y=3/22 plus the old font clone shifts (small_tall 21/16, ascii 2).
 */
public final class TitleHud {
    public static final int LINE_1_Y = 3;
    public static final int LINE_2_Y = 22;
    public static final int LINE_1_SMALL_Y = LINE_1_Y + 21;
    public static final int LINE_1_TEXT_Y = LINE_1_Y + 2;
    public static final int LINE_2_SMALL_Y = LINE_2_Y + 16;

    private static final BackgroundSpriteSet BG_LINE_1 = new BackgroundSpriteSet("hud/bossbar/line1");
    private static final BackgroundSpriteSet BG_LINE_2 = new BackgroundSpriteSet("hud/bossbar/line2");
    private static final int BORDER_WIDTH = 4;

    public static final Component ADDRESS_LINE = line2(small2("hollowcube.net", NamedTextColor.WHITE));

    private TitleHud() {
    }

    public static @NotNull HudBar.Module module(@NotNull Component... lines) {
        var content = Component.text();
        for (var line : lines) content.append(line);
        return HudBar.staticModule(content.build());
    }

    // Text pieces

    public static @NotNull Component small1(@NotNull String text, @NotNull TextColor color) {
        return HudText.anchored(Component.text(FontUtil.rewrite("small_tall", text), color), HudAnchor.TOP, LINE_1_SMALL_Y);
    }

    public static @NotNull Component small2(@NotNull String text, @NotNull TextColor color) {
        return HudText.anchored(Component.text(FontUtil.rewrite("small_tall", text), color), HudAnchor.TOP, LINE_2_SMALL_Y);
    }

    public static @NotNull Component text1(@NotNull Component text) {
        return HudText.anchored(text, HudAnchor.TOP, LINE_1_TEXT_Y);
    }

    // Lines

    public static @NotNull Component line1(@NotNull Component text) {
        return line(text, BG_LINE_1, LINE_1_Y);
    }

    public static @NotNull Component line2(@NotNull Component text) {
        return line(text, BG_LINE_2, LINE_2_Y);
    }

    private static @NotNull Component line(@NotNull Component text, @NotNull BackgroundSpriteSet bg, int bgY) {
        int contentWidth = FontUtil.measureText(text);
        var bgString = bg.build(contentWidth + (2 * BORDER_WIDTH));
        int bgAdvance = bg.advance(contentWidth + (2 * BORDER_WIDTH));

        // Center on the line's net advance like vanilla centered the old boss bar name.
        int netAdvance = bgAdvance - BORDER_WIDTH - 2;
        int start = -(netAdvance / 2);
        int end = start + netAdvance;

        return Component.text()
                .append(Component.text(FontUtil.computeOffset(start)))
                .append(HudText.anchored(Component.text(bgString), HudAnchor.TOP, bgY))
                .append(Component.text(FontUtil.computeOffset(-(contentWidth + BORDER_WIDTH + 2))))
                .append(text)
                .append(Component.text(FontUtil.computeOffset(-end)))
                .build();
    }

    // Module factories

    public static @NotNull HudBar.Module playing(@NotNull PlayerClient players, @NotNull MapData map) {
        Component ownerName;
        try {
            ownerName = players.getDisplayName(map.owner())
                    .build(DisplayName.Context.BOSS_BAR);
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
            ownerName = text1(Component.text("!error!", NamedTextColor.RED));
        }

        var line1 = line1(map.verification() == MapVerification.PENDING
                ? Component.text()
                .append(small1("verifying", NamedTextColor.WHITE)).append(Component.text(" "))
                .append(text1(Component.text(map.name(), TextColor.color(0xF2F2F2)))).build()
                : Component.text()
                .append(small1("playing", NamedTextColor.WHITE)).append(Component.text(" "))
                .append(text1(MapData.rewriteWithQualityFont(map.quality(), map.name())))
                .append(Component.text(" ")).append(small1("by", TextColor.color(0xB0B0B0))).append(Component.text(" "))
                .append(ownerName).build());

        var line2 = !map.isPublished() ? ADDRESS_LINE
                : line2(Component.text()
                .append(small2("/play", NamedTextColor.WHITE))
                .appendSpace()
                .append(small2(map.publishedIdString(), NamedTextColor.WHITE))
                .appendSpace()
                .append(small2("on", TextColor.color(0xB0B0B0)))
                .appendSpace()
                .append(small2("hollowcube.net", NamedTextColor.WHITE))
                .build());

        return module(line1, line2);
    }

    public static @NotNull HudBar.Module editing(@NotNull MapData map) {
        var builder = Component.text()
                .append(small1("building", NamedTextColor.WHITE)).append(Component.text(" "))
                .append(text1(Component.text(map.name(), TextColor.color(0x30FBFF))));

//        final String permissionName = player.getUuid().toString().equals(map().owner()) ? "Owner" : "Builder";
//        builder.append(small1("  permission ", TextColor.color(0xB0B0B0)))
//                .append(text1(Component.text(permissionName, NamedTextColor.WHITE)));

        return module(line1(builder.build()), ADDRESS_LINE);
    }
}
