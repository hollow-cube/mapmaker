package net.hollowcube.mapmaker.chat;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        var channel = PlayerDataV2.fromPlayer(player).getSetting(PlayerSettings.CHAT_CHANNEL);
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
