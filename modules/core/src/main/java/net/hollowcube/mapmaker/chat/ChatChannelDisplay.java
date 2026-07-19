package net.hollowcube.mapmaker.chat;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.entity.Player;

public class ChatChannelDisplay implements PlayerHud.Module {

    public static final ChatChannelDisplay INSTANCE = new ChatChannelDisplay();

    private static final int NORMAL_OFFSET = -98;
    private static final int OFF_HAND_OFFSET = -127;

    private static final BadSprite GLOBAL = BadSprite.require("hud/chat/global_channel");
    private static final BadSprite LOCAL = BadSprite.require("hud/chat/local_channel");
    private static final BadSprite STAFF = BadSprite.require("hud/chat/staff_channel");

    private ChatChannelDisplay() {
    }

    @Override
    public HudNode.Anchored render(Player player) {
        var channel = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_CHANNEL);
        var sprite = switch (channel) {
            case ClientChatMessageData.CHANNEL_LOCAL -> LOCAL;
            case ClientChatMessageData.CHANNEL_STAFF -> STAFF;
            default -> GLOBAL;
        };
        var offset = player.getItemInOffHand().isAir() ? NORMAL_OFFSET : OFF_HAND_OFFSET;

        return HudNode.sprite(sprite)
            .frame(0, HudNode.Align.RIGHT)
            .offset(offset, -20)
            .anchored(HudAnchor.BOTTOM);
    }
}
