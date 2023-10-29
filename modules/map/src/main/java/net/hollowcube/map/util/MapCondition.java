package net.hollowcube.map.util;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MapCondition {

    public static @NotNull CommandCondition mapFilter(boolean playing, boolean editing, boolean testing) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) return CommandCondition.HIDE;

            if (playing && (world.flags() & MapWorld.FLAG_PLAYING) == 0) return CommandCondition.HIDE;
            if (editing && (world.flags() & MapWorld.FLAG_EDITING) == 0) return CommandCondition.HIDE;
            if (testing && (world.flags() & MapWorld.FLAG_TESTING) == 0) return CommandCondition.HIDE;

            return CommandCondition.ALLOW;
        };
    }
    
}
