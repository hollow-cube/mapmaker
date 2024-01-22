package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
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

    private static final Component FADEOUT_TITLE = Component.text(BadSprite.SPRITE_MAP.get("hud/fadeout").fontChar());
    private static final Title.Times FADEOUT_TIMES = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(15000), Duration.ofMillis(0));

    public static void sendFadeout(@NotNull Player player) {
        player.showTitle(Title.title(FADEOUT_TITLE, Component.empty(), FADEOUT_TIMES));
    }

    public static void assignTeam(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);
        player.setTeam(switch (playerData.displayName2().getBadgeName()) {
            case "dev_3", "mod_3", "ct_3" -> CoreTeams.RED;
            case "dev_2", "mod_2", "ct_2" -> CoreTeams.CYAN;
            case "dev_1", "mod_1", "ct_1" -> CoreTeams.GREEN;
            case null, default -> CoreTeams.DEFAULT;
        });
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

    private static final BadSprite CURRENCY_DISPLAY = BadSprite.SPRITE_MAP.get("hud/currency_display");

    public static void buildCurrencyDisplay(@NotNull Player p, @NotNull FontUIBuilder builder) {
        // Never show in spectator. It generally makes no sense, but also Axiom uses spectator when in editor mode,
        // which should not show this ui for sure (it looks awful).
        if (p.getGameMode() == GameMode.SPECTATOR) return;

        var playerData = PlayerDataV2.fromPlayer(p);

        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(11).drawInPlace(CURRENCY_DISPLAY);

        int MAX_TEXT_WIDTH = 22;

        var coinText = NumberUtil.formatCurrency(playerData.coins());
        builder.pos(15 + (MAX_TEXT_WIDTH - FontUtil.measureText("currency", coinText))).append("currency", coinText);
        var cubitText = NumberUtil.formatCurrency(playerData.cubits());
        builder.pos(56 + (MAX_TEXT_WIDTH - FontUtil.measureText("currency", cubitText))).append("currency", cubitText);
    }

    private static final BadSprite XP_BAR_BACKGROUND = BadSprite.SPRITE_MAP.get("hud/level/xp_bar_background");

    public static void buildExperienceBar(@NotNull Player p, @NotNull FontUIBuilder builder) {
        // Never show in spectator. It generally makes no sense, but also Axiom uses spectator when in editor mode,
        // which should not show this ui for sure (it looks awful).
        if (p.getGameMode() == GameMode.SPECTATOR) return;

        var hasExperienceBar = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
        if (hasExperienceBar) return; // Use the builtin one for these.

        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(-(XP_BAR_BACKGROUND.width() / 2)).drawInPlace(XP_BAR_BACKGROUND);
    }

    @Blocking
    public static @Nullable MapData getCurrentMap(@NotNull SessionManager sessionManager, @NotNull MapService mapService, @NotNull Player player) {
        var playerId = PlayerDataV2.fromPlayer(player).id();
        return getCurrentMap(sessionManager, mapService, playerId);
    }

    @Blocking
    public static @Nullable MapData getCurrentMap(@NotNull SessionManager sessionManager, @NotNull MapService mapService, @NotNull String playerId) {
        var presence = sessionManager.getPresence(playerId);
        if (presence == null || !presence.type().equals(MapPresence.TYPE)) return null;
        return mapService.getMap(playerId, presence.mapId());
    }

    public static void applyCosmetics(@NotNull Player player, @NotNull PlayerDataV2 playerData) {
        var head = Cosmetic.byId(CosmeticType.HEAD, playerData.getCosmetic(CosmeticType.HEAD));
        player.getInventory().setHelmet(head == null ? ItemStack.AIR : head.icon());

//        var newDisplayedSkinParts = player.getSettings().getDisplayedSkinParts();
//        newDisplayedSkinParts &= ~0x40;
//        player.getSettings().refresh(
//                player.getSettings().getLocale(),
//                player.getSettings().getViewDistance(),
//                player.getSettings().getChatMessageType(),
//                player.getSettings().hasChatColors(),
//                newDisplayedSkinParts,
//                player.getSettings().getMainHand(),
//                player.getSettings().enableTextFiltering(),
//                player.getSettings().allowServerListings()
//        );
    }
}
