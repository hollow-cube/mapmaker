package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static net.hollowcube.command.CommandCondition.ALLOW;
import static net.hollowcube.command.CommandCondition.HIDE;

public final class MapConditions {

    public static @NotNull CommandCondition playerFeature(@NotNull FeatureFlag flag) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return HIDE;
            return flag.test(player.getUuid().toString()) ? ALLOW : HIDE;
        };
    }

    public static @NotNull CommandCondition mapFeature(@NotNull FeatureFlag flag) {
        return map(world -> flag.test(world.map()));
    }

    public static @NotNull CommandCondition map(Predicate<MapWorld> predicate) {
        return mapPlayer((world, _) -> predicate.test(world));
    }

    public static @NotNull CommandCondition mapPlayer(BiPredicate<MapWorld, Player> predicate) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return HIDE;
            var world = MapWorld.forPlayer(player);
            return world != null && predicate.test(world, player) ? ALLOW : HIDE;
        };
    }

}
