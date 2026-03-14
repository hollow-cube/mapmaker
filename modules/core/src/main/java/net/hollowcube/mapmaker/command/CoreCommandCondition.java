package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.MagicConstant;

import static net.hollowcube.command.CommandCondition.*;

public final class CoreCommandCondition {

    public static final CommandCondition IN_STAFF_MODE = (sender, _) ->
        sender instanceof Player p && PlayerData.fromPlayer(p).getSetting(PlayerSettings.STAFF_MODE)
            ? CommandCondition.ALLOW : CommandCondition.HIDE;

    public static CommandCondition perm(@MagicConstant(flagsFromClass = Permission.class) long perm) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return HIDE;
            return PlayerData.fromPlayer(player).has(perm) ? ALLOW : HIDE;
        };
    }

    public static CommandCondition staffPerm(@MagicConstant(flagsFromClass = Permission.class) long perm) {
        return and(IN_STAFF_MODE, perm(perm));
    }

    public static CommandCondition playerFeature(FeatureFlag flag) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return HIDE;
            return flag.test(player) ? ALLOW : HIDE;
        };
    }

    private CoreCommandCondition() {
    }
}
