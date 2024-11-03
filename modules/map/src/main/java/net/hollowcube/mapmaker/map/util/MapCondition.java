package net.hollowcube.mapmaker.map.util;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static net.hollowcube.command.CommandCondition.ALLOW;
import static net.hollowcube.command.CommandCondition.HIDE;

public final class MapCondition {
    //todo(mattw) check over these i think i broke them tbh

    public static @NotNull Predicate<InstanceEvent> eventFilter(boolean playing, boolean editing, boolean testing) {
        return event -> {
            MapWorld world;
            if (event instanceof PlayerEvent playerEvent)
                world = MapWorld.forPlayerOptional(playerEvent.getPlayer());
            else world = MapWorld.unsafeFromInstance(event.getInstance());
            if (world == null) return false;

            if (event instanceof PlayerEvent playerEvent) {
                var player = playerEvent.getPlayer();
                if (playing && !(world instanceof PlayingMapWorld || world.isPlaying(player))) return false;
                if (editing && !world.canEdit(player)) return false;
                if (testing && !(world instanceof TestingMapWorld || !world.isPlaying(player))) return false;
            } else {
                if (playing && !(world instanceof PlayingMapWorld)) return false;
                if (editing && !(world instanceof EditingMapWorld)) return false;
                if (testing && !(world instanceof TestingMapWorld)) return false;
            }

            return true;
        };
    }

    /**
     * Never matches spectators to maintain previous behavior.
     */
    public static @NotNull CommandCondition mapFilter(boolean playing, boolean editing, boolean testing) {
        return mapFilter(playing, editing, testing, false);
    }

    public static @NotNull CommandCondition mapFilter(boolean playing, boolean editing, boolean testing, boolean allowSpectators) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) return HIDE;
            var world = MapWorld.forPlayerOptional(player);
            return switch (world) {
                case PlayingMapWorld _ ->
                        playing && (world.isPlaying(player) || (allowSpectators && world.isSpectating(player))) ? ALLOW : HIDE;
                case EditingMapWorld _ -> editing && world.canEdit(player) ? ALLOW : HIDE;
                case TestingMapWorld _ ->
                        testing && (world.isPlaying(player) || (allowSpectators && world.isSpectating(player))) ? ALLOW : HIDE;
                case null, default -> HIDE;
            };
        };
    }

    public static @NotNull CommandCondition mapFeature(@NotNull FeatureFlag flag) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) return HIDE;
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) return HIDE;

            return flag.test(world.map()) ? ALLOW : HIDE;
        };
    }

}
