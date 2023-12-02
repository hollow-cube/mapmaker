package net.hollowcube.map.util;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static net.hollowcube.command.CommandCondition.ALLOW;
import static net.hollowcube.command.CommandCondition.HIDE;

public final class MapCondition {

    public static @NotNull Predicate<InstanceEvent> eventFilter(boolean playing, boolean editing, boolean testing) {
        return event -> {
            MapWorld world;
            if (event instanceof PlayerEvent playerEvent)
                world = MapWorld.forPlayerOptional(playerEvent.getPlayer());
            else world = MapWorld.unsafeFromInstance(event.getInstance());
            if (world == null) return false;

            if (playing && (world.flags() & MapWorld.FLAG_PLAYING) == 0) return false;
            if (editing && (world.flags() & MapWorld.FLAG_EDITING) == 0) return false;
            if (testing && (world.flags() & MapWorld.FLAG_TESTING) == 0) return false;

            return true;
        };
    }

    public static @NotNull CommandCondition mapFilter(boolean playing, boolean editing, boolean testing) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) return HIDE;
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) return HIDE;

            if (playing && (world.flags() & MapWorld.FLAG_PLAYING) == 0) return HIDE;
            if (editing && (world.flags() & MapWorld.FLAG_EDITING) == 0) return HIDE;
            if (testing && (world.flags() & MapWorld.FLAG_TESTING) == 0) return HIDE;

            return ALLOW;
        };
    }

}
