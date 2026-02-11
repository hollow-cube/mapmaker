package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.ALLOW;
import static net.hollowcube.command.CommandCondition.HIDE;

public class CoreCommandCondition {

    public static @NotNull CommandCondition playerFeature(@NotNull FeatureFlag flag) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return HIDE;
            return flag.test(player) ? ALLOW : HIDE;
        };
    }
}
