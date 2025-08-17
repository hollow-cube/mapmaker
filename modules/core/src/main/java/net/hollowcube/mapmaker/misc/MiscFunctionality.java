package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

public final class MiscFunctionality {

    private static final Component FADEOUT_TITLE = Component.text(BadSprite.SPRITE_MAP.get("hud/fadeout").fontChar());
    private static final Title.Times FADEOUT_TIMES = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(15000), Duration.ofMillis(0));

    public static void sendFadeout(@NotNull Player player) {
        player.showTitle(Title.title(FADEOUT_TITLE, Component.empty(), FADEOUT_TIMES));
    }

    public static void assignTeam(@NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);
        player.setTeam(CoreTeams.DEFAULT);
//        player.setTeam(switch (playerData.displayName2().getBadgeName()) {
//            case "dev_3", "mod_3", "ct_3" -> CoreTeams.RED;
//            case "dev_2", "mod_2", "ct_2" -> CoreTeams.CYAN;
//            case "dev_1", "mod_1", "ct_1" -> CoreTeams.GREEN;
//            case null, default -> CoreTeams.DEFAULT;
//        });
    }

    public static void broadcastTabList(@NotNull Audience audience, int onlinePlayers) {
        var playersText = onlinePlayers == 1 ? "ᴘʟᴀʏᴇʀ" : "ᴘʟᴀʏᴇʀѕ";
        var playerCountText = FontUtil.rewrite("smallnums", "" + onlinePlayers);

        var blueColor = TextColor.color(56, 140, 249);
        var goldColor = TextColor.color(235, 188, 53);
        var darkGrayColor = TextColor.color(0x696969);
        var lightGrayColor = TextColor.color(0xB0B0B0); // or cccccc

        var tabLogoSprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/tab/logo_outline"));
        var cubeOffset = FontUtil.computeOffset(tabLogoSprite.width() + FontUtil.measureText(" Hollow Cube") - 60); //todo where is the missing 2 coming from
        var tabHeader = Component.text()
                .appendNewline()
                .appendNewline()
                .appendNewline()
                .append(Component.empty()
                        .append(Component.text(FontUtil.computeOffset(-15) + tabLogoSprite.fontChar()).shadowColor(ShadowColor.none()))
                        .append(Component.text(" Hollow Cube", blueColor))).appendNewline()
                .append(Component.text(cubeOffset + "ᴇᴀʀʟʏ ᴀᴄᴄᴇѕѕ", darkGrayColor))
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

        var playerData = PlayerData.fromPlayer(p);

        builder.pushShadowColor(ShadowColor.none());
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

        builder.pushShadowColor(ShadowColor.none());
        builder.pos(-(XP_BAR_BACKGROUND.width() / 2)).drawInPlace(XP_BAR_BACKGROUND);
    }

    @Blocking
    public static @Nullable MapData getCurrentMap(@NotNull SessionManager sessionManager, @NotNull MapService mapService, @NotNull Player player) {
        var playerId = PlayerData.fromPlayer(player).id();
        return getCurrentMap(sessionManager, mapService, playerId);
    }

    @Blocking
    public static @Nullable MapData getCurrentMap(@NotNull SessionManager sessionManager, @NotNull MapService mapService, @NotNull String playerId) {
        var presence = sessionManager.getPresence(playerId);
        if (presence == null || !presence.type().equals(MapPresence.TYPE)) return null;
        return mapService.getMap(playerId, presence.mapId());
    }

    public static void applyCosmetics(@NotNull Player player, @NotNull PlayerData playerData) {
        for (var cosmeticType : CosmeticType.VALUES) {
            var cosmetic = Cosmetic.byId(cosmeticType, playerData.getCosmetic(cosmeticType));
            var itemStack = cosmetic == null ? cosmeticType.blankIcon() : cosmetic.impl().iconItem();
            // If the itemstack has a glider we need to preserve it.
            if (player.getInventory().getItemStack(cosmeticType.iconSlot()).has(DataComponents.GLIDER)) {
                itemStack = itemStack.with(DataComponents.GLIDER);
                var equippable = itemStack.get(DataComponents.EQUIPPABLE);
                if (equippable != null) itemStack = itemStack.with(DataComponents.EQUIPPABLE,
                        equippable.withAssetId("minecraft:elytra"));
            }
            player.getInventory().setItemStack(cosmeticType.iconSlot(), itemStack);
        }
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
