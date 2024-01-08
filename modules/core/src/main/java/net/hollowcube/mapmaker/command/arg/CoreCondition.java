package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.ALLOW;
import static net.hollowcube.command.CommandCondition.HIDE;
import static net.hollowcube.mapmaker.feature.FeatureFlag.player;

public final class CoreCondition {

    public static @NotNull CommandCondition feature(@NotNull FeatureFlag flag) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) return HIDE;
            return flag.test(player(player)) ? ALLOW : HIDE;
        };
    }

}
