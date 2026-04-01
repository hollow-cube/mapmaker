package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.chat.components.ChatLanguage;
import net.hollowcube.mapmaker.map.VisibilityRule;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;

public final class PlayerSettings {
    // Note that unlike it claims, this actually is not all declarations of player settings.
    // It is probably worth moving them all here in the future for consistency.

    // General Settings

    public static final PlayerSetting<String> CHAT_CHANNEL = PlayerSetting.String("chat_channel", ClientChatMessageData.CHANNEL_GLOBAL);
    public static final PlayerSetting<Boolean> COSMETICS_SHOW_LOCKED = PlayerSetting.Bool("cosmetics.show_locked", true);
    public static final PlayerSetting<ChatLanguage> CHAT_LANGUAGE = PlayerSetting.Enum("chat_language", ChatLanguage.ORIGINAL);

    // Hub

    // Note that this is keyed badly (ie to general not indicating its a hub option),
    // but changing it is a pain so it will remaind :)
    public static final PlayerSetting<Integer> HUB_SELECTED_SLOT = PlayerSetting.Int("selected_slot", 0);


    // Playing (parkour) maps

    public static final PlayerSetting<VisibilityRule> NEARBY_PLAYER_VISIBILITY = PlayerSetting.Enum("nearby_player_visibility", VisibilityRule.GHOST);


    // Moderation

    public static PlayerSetting<Boolean> IS_VANISHED = PlayerSetting.Bool("is_hidden", false);
    public static PlayerSetting<Boolean> STAFF_MODE = PlayerSetting.Bool("staff_mode", true);

    // Terraform

    public static PlayerSetting<Boolean> ENABLE_WE_CUI = PlayerSetting.Bool("we_outline_cui", false);

    // Social

    public static PlayerSetting<Boolean> AUTO_REJECT_FRIEND_REQUESTS = PlayerSetting.Bool("auto_reject_friend_requests", false);
    public static PlayerSetting<Boolean> ALLOW_DIRECT_MESSAGES = PlayerSetting.Bool("allow_direct_messages", true);
    public static PlayerSetting<Boolean> ENABLE_PING_SOUNDS = PlayerSetting.Bool("enable_ping_sounds", true);

}
