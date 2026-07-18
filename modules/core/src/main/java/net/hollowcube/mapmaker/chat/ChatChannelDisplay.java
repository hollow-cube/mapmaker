package net.hollowcube.mapmaker.chat;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudBar;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatChannelDisplay implements HudBar.Module {

    public static final ChatChannelDisplay INSTANCE = new ChatChannelDisplay();

    private static final int NORMAL_OFFSET = -96;
    private static final int OFF_HAND_OFFSET = -125;
    // Replaces the sprites' old texture-encoded shift_y of 52 (relative to the actionbar row at -72).
    private static final int OFFSET_Y = -20;

    private static final BadSprite GLOBAL = BadSprite.require("hud/chat/global_channel");
    private static final BadSprite LOCAL = BadSprite.require("hud/chat/local_channel");
    private static final BadSprite STAFF = BadSprite.require("hud/chat/staff_channel");

    private ChatChannelDisplay() {
    }

    @Override
    public int cacheKey(@NotNull Player player) {
        var channel = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_CHANNEL);
        var offset = player.getItemInOffHand().isAir() ? NORMAL_OFFSET : OFF_HAND_OFFSET;
        return channel.hashCode() * 31 + offset;
    }

    @Override
    public @NotNull Component render(@NotNull Player player) {
        var channel = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_CHANNEL);
        var sprite = switch (channel) {
            case ClientChatMessageData.CHANNEL_LOCAL -> LOCAL;
            case ClientChatMessageData.CHANNEL_STAFF -> STAFF;
            default -> GLOBAL;
        };
        var offset = player.getItemInOffHand().isAir() ? NORMAL_OFFSET : OFF_HAND_OFFSET;

        var builder = new FontUIBuilder();
        builder.pushColor(HudText.KILL);
        builder.pushShadowColor(HudText.marker(HudAnchor.BOTTOM, OFFSET_Y, NamedTextColor.WHITE));
        builder.pos(offset);
        builder.offset(-sprite.width());
        builder.append(Character.toString(sprite.fontChar()));
        builder.popShadowColor();
        builder.popColor();
        return builder.build(true);
    }
}
