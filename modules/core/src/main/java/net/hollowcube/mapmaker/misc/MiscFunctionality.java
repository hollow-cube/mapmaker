package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MiscFunctionality {
    public static void sendBetaHeader(@NotNull Player player) {
        var runtime = ServerRuntime.getRuntime();
        String watermarkString = String.format("play.hollowcube.net • Closed Beta (%s)", runtime.shortCommit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString).color(FontUtil.NO_SHADOW), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
        player.showBossBar(BossBar.bossBar(Component.text(FontUtil.rewrite("small_bossbar_line2", "not representative of final product"))
                .color(FontUtil.NO_SHADOW), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
    }

    public static void broadcastTabList(@NotNull Audience audience) {
        broadcastTabList(audience, MinecraftServer.getConnectionManager().getOnlinePlayerCount());
    }

    public static void broadcastTabList(@NotNull Audience audience, int onlinePlayers) {
        var playersText = onlinePlayers == 1 ? "ᴘʟᴀʏᴇʀ" : "ᴘʟᴀʏᴇʀѕ";
        var playerCountText = FontUtil.rewrite("smallnums", "" + onlinePlayers);

        var blueColor = TextColor.color(56, 140, 249);
        var goldColor = TextColor.color(235, 188, 53);
        var darkGrayColor = TextColor.color(0x696969);
        var lightGrayColor = TextColor.color(0xB0B0B0); // or cccccc

        var tabLogoSprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/tab/logo_outline"));
        var cubeOffset = FontUtil.computeOffset(tabLogoSprite.width() + FontUtil.measureText(" Hollow Cube") - 50); //todo where is the missing 2 coming from
        var tabHeader = Component.text()
                .appendNewline()
                .append(Component.text(tabLogoSprite.fontChar(), FontUtil.NO_SHADOW).append(Component.text(" Hollow Cube", blueColor))).appendNewline()
                .append(Component.text(cubeOffset + "ᴄʟᴏѕᴇᴅ ʙᴇᴛᴀ", darkGrayColor))
                .appendNewline()
                .build();
        var tabFooter = Component.text()
                .appendNewline()
                .append(Component.text("ᴘʟᴀʏ.", lightGrayColor).append(Component.text("ʜᴏʟʟᴏᴡᴄᴜʙᴇ", goldColor)).append(Component.text(".ɴᴇᴛ", lightGrayColor))).appendNewline()
                .append(Component.text(playerCountText, blueColor).append(Component.text(" " + playersText + " ᴏɴʟɪɴᴇ", darkGrayColor))).appendNewline()
                .append(Component.text(FontUtil.computeOffset(125))) // Min width
                .build();

        audience.sendPlayerListHeaderAndFooter(tabHeader, tabFooter);
    }

    private static final BadSprite CURRENCY_DISPLAY_SURVIVAL = BadSprite.SPRITE_MAP.get("hud/currency_display");
    private static final BadSprite CURRENCY_DISPLAY_CREATIVE = BadSprite.SPRITE_MAP.get("hud/currency_display_creative");

    public static void buildCurrencyDisplay(@NotNull Player p, @NotNull FontUIBuilder builder) {
        var hasExperienceBar = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
        var playerData = PlayerDataV2.fromPlayer(p);

        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(11).drawInPlace(hasExperienceBar ? CURRENCY_DISPLAY_SURVIVAL : CURRENCY_DISPLAY_CREATIVE);

        int MAX_TEXT_WIDTH = 22;
        var font = hasExperienceBar ? "currency" : "currency_creative";

        var coinText = NumberUtil.formatCurrency(playerData.coins());
        builder.pos(15 + (MAX_TEXT_WIDTH - FontUtil.measureText(font, coinText))).append(font, coinText);
        var cubitText = NumberUtil.formatCurrency(playerData.cubits());
        builder.pos(56 + (MAX_TEXT_WIDTH - FontUtil.measureText(font, cubitText))).append(font, cubitText);
    }
}
