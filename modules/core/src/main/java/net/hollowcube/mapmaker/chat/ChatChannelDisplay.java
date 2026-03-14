package net.hollowcube.mapmaker.chat;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;

public class ChatChannelDisplay implements ActionBar.Provider {

    public static final ChatChannelDisplay INSTANCE = new ChatChannelDisplay();

    private static final int NORMAL_OFFSET = -96;
    private static final int OFF_HAND_OFFSET = -125;

    private static final BadSprite GLOBAL = BadSprite.require("hud/chat/global_channel");
    private static final BadSprite LOCAL = BadSprite.require("hud/chat/local_channel");
    private static final BadSprite STAFF = BadSprite.require("hud/chat/staff_channel");

    private ChatChannelDisplay() {
    }

    @Override
    public int cacheKey(Player player) {
        var channel = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_CHANNEL);
        var offset = player.getItemInOffHand().isAir() ? NORMAL_OFFSET : OFF_HAND_OFFSET;
        return channel.hashCode() * 31 + offset;
    }

    @Override
    public void provide(Player player, FontUIBuilder builder) {
        var channel = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_CHANNEL);
        var sprite = switch (channel) {
            case ClientChatMessageData.CHANNEL_LOCAL -> LOCAL;
            case ClientChatMessageData.CHANNEL_STAFF -> STAFF;
            default -> GLOBAL;
        };
        var offset = player.getItemInOffHand().isAir() ? NORMAL_OFFSET : OFF_HAND_OFFSET;

        builder.pos(offset);
        builder.offset(-sprite.width());
        builder.pushShadowColor(ShadowColor.none());
        builder.append(Character.toString(sprite.fontChar()));
        builder.popShadowColor();
    }
}
