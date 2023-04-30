package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorldNew;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.location.RelativeVec;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand extends BaseMapCommand {
    private final Argument<RelativeVec> positionArg = ArgumentType.RelativeVec3("position");
    private final Argument<RelativeVec> rotationArg = ArgumentType.RelativeVec2("position");

    public SetSpawnCommand() {
        super(true, "setspawn");

        addSyntax(this::setSpawn);
        addSyntax(this::setSpawnWithPos, positionArg);
        addSyntax(this::setSpawnWithPosAndRot, positionArg, rotationArg);
    }

    private void setSpawn(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        updateMapPos(player, player.getPosition());
    }

    private void setSpawnWithPos(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var pos = context.get(positionArg).fromSender(player);
        updateMapPos(player, new Pos(pos.x(), pos.y(), pos.z(),
                player.getPosition().yaw(), player.getPosition().pitch()));
    }

    private void setSpawnWithPosAndRot(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var pos = context.get(positionArg).fromSender(player);
        var rot = context.get(rotationArg).fromView(player);

        updateMapPos(player, new Pos(pos.x(), pos.y(), pos.z(), (float) rot.x(), (float) rot.z()));
    }

    private void updateMapPos(@NotNull Player player, @NotNull Pos newSpawnPoint) {
        var map = MapWorldNew.forPlayer(player).map();
        map.setSpawnPoint(newSpawnPoint);
        player.sendMessage(MapMessages.COMMAND_SETSPAWN_SUCCESS.with(
                Component.text(newSpawnPoint.blockX()).hoverEvent(Component.text(newSpawnPoint.x(), NamedTextColor.DARK_AQUA)),
                Component.text(newSpawnPoint.blockY()).hoverEvent(Component.text(newSpawnPoint.y(), NamedTextColor.DARK_AQUA)),
                Component.text(newSpawnPoint.blockZ()).hoverEvent(Component.text(newSpawnPoint.z(), NamedTextColor.DARK_AQUA)),
                Component.text(Math.floor(newSpawnPoint.pitch())).hoverEvent(Component.text(newSpawnPoint.pitch(), NamedTextColor.DARK_AQUA)),
                Component.text(Math.floor(newSpawnPoint.yaw())).hoverEvent(Component.text(newSpawnPoint.yaw(), NamedTextColor.DARK_AQUA))
        ));
    }
}
