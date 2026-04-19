package net.hollowcube.mapmaker.gui.settings;

import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.map.VisibilityRule;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.List;

public class PlayerSettingsOptions {

    public static final Key SETTINGS_DIALOG_ID = Key.key("hollowcube:player_settings");
    public static final int OPTION_WIDTH = 225;

    public static final List<PlayerSettingsOption> OPTIONS = List.of(
        PlayerSettingsOption.forBool(
            PlayerSettings.ENABLE_WE_CUI,
            Component.translatable("dialog.settings.option.cui")
        ),
        PlayerSettingsOption.forSelect(
            PlayerSettings.CHAT_CHANNEL,
            Component.translatable("dialog.settings.option.chat"),
            ClientChatMessageData.CHANNEL_GLOBAL, ClientChatMessageData.CHANNEL_LOCAL
        ),
        PlayerSettingsOption.forEnum(
            PlayerSettings.NEARBY_PLAYER_VISIBILITY,
            Component.translatable("dialog.settings.option.visibility"),
            VisibilityRule.class
        ),
        PlayerSettingsOption.forBool(
            PlayerSettings.AUTO_REJECT_FRIEND_REQUESTS,
            Component.translatable("dialog.settings.option.auto_reject_friend_requests")
        ),
        PlayerSettingsOption.forBool(
            PlayerSettings.ALLOW_DIRECT_MESSAGES,
            Component.translatable("dialog.settings.option.allow_direct_messages")
        ),
        PlayerSettingsOption.forBool(
            PlayerSettings.ALLOW_BUILDER_INVITES,
            Component.translatable("dialog.settings.option.allow_builder_invites")
        ),
        PlayerSettingsOption.forBool(
            PlayerSettings.ENABLE_PING_SOUNDS,
            Component.translatable("dialog.settings.option.enable_ping_sounds")
        )
    );
}
