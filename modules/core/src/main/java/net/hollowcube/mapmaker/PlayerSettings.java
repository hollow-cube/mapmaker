package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.map.VisibilityRule;
import net.hollowcube.mapmaker.player.PlayerSetting;

public final class PlayerSettings {
    // Note that unlike it claims, this actually is not all declarations of player settings.
    // It is probably worth moving them all here in the future for consistency.


    // Playing (parkour) maps

    public static PlayerSetting<VisibilityRule> NEARBY_PLAYER_VISIBILITY = PlayerSetting.Enum("nearby_player_visibility", VisibilityRule.GHOST);


    // Moderation

    public static PlayerSetting<Boolean> IS_VANISHED = PlayerSetting.Bool("is_hidden", false);

}
