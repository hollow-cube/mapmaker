package net.hollowcube.map.command.build;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class SetSpawnCommand extends Command {

    public SetSpawnCommand() {
        super("setspawn");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleSetSpawnToPlayer));
        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("other syntax todo");
        });
    }

    private void handleSetSpawnToPlayer(@NotNull Player player, @NotNull CommandContext context) {
        updateMapPos(player, player.getPosition());
    }

    private void updateMapPos(@NotNull Player player, @NotNull Pos newSpawnPoint) {
        var map = MapWorld.forPlayer(player).map();
        map.settings().setSpawnPoint(newSpawnPoint);
        player.sendMessage(MapMessages.COMMAND_SETSPAWN_SUCCESS.with(
                Component.text(newSpawnPoint.blockX()).hoverEvent(Component.text(newSpawnPoint.x(), NamedTextColor.DARK_AQUA)),
                Component.text(newSpawnPoint.blockY()).hoverEvent(Component.text(newSpawnPoint.y(), NamedTextColor.DARK_AQUA)),
                Component.text(newSpawnPoint.blockZ()).hoverEvent(Component.text(newSpawnPoint.z(), NamedTextColor.DARK_AQUA)),
                Component.text(Math.floor(newSpawnPoint.pitch())).hoverEvent(Component.text(newSpawnPoint.pitch(), NamedTextColor.DARK_AQUA)),
                Component.text(Math.floor(newSpawnPoint.yaw())).hoverEvent(Component.text(newSpawnPoint.yaw(), NamedTextColor.DARK_AQUA))
        ));
    }
}
