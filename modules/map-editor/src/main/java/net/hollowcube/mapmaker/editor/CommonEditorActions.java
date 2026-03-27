package net.hollowcube.mapmaker.editor;

import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import static net.kyori.adventure.text.Component.translatable;

/**
 * This class should be used when handling common actions between commands and items while a user is in editor mode.
 */
public class CommonEditorActions {

    public static void trySetSpawn(Player player, Pos newSpawnPoint) {
        if (!CoordinateUtil.inBorder(player.getInstance().getWorldBorder(), newSpawnPoint, 2)) {
            player.sendMessage(translatable("command.set_spawn.out_of_world"));
            return;
        }

        if (!CoordinateUtil.withinYLimit(player.getInstance(), newSpawnPoint)) {
            player.sendMessage(translatable("command.set_spawn.outside_height"));
            return;
        }

        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        world.setSpawnPoint(newSpawnPoint);
        player.sendMessage(translatable("command.set_spawn.success", CoordinateUtil.asTranslationArgs(newSpawnPoint)));
    }
}
