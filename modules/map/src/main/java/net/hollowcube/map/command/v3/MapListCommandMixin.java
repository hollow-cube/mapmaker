package net.hollowcube.map.command.v3;

import net.hollowcube.command.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Adds the `/map info` command for viewing info about your current map.
 */
public final class MapListCommandMixin {

    public static void showMapInfoAboutCurrent(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("show info about current");
    }

    private MapListCommandMixin() {
    }
}
