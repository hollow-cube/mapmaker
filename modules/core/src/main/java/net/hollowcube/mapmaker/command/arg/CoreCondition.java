package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.minestom.server.entity.Player;

import static net.hollowcube.command.CommandCondition.ALLOW;
import static net.hollowcube.command.CommandCondition.HIDE;

public final class CoreCondition {

    public static CommandCondition feature(FeatureFlag flag) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return HIDE;
            return flag.test(player) ? ALLOW : HIDE;
        };
    }

}
