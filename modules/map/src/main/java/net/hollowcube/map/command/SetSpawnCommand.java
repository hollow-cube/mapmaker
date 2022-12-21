package net.hollowcube.map.command;

import net.hollowcube.map.world.MapWorld;
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
        addSyntax(this::setSpawnWithPos, positionArg, rotationArg);
    }

    private void setSpawn(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) return;
        var newSpawnPoint = player.getPosition();

        var map = MapWorld.fromInstance(player.getInstance()).map();
        map.setSpawnPoint(newSpawnPoint);
        player.sendMessage("Set spawn to " + newSpawnPoint); //todo translation
    }

    private void setSpawnWithPos(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) return;

        var pos = context.get(positionArg).fromSender(player);
        var rot = context.get(positionArg).fromView(player);
        var newSpawnPoint = new Pos(pos.x(), pos.y(), pos.z(), (float) rot.x(), (float) rot.z());

        var map = MapWorld.fromInstance(player.getInstance()).map();
        map.setSpawnPoint(newSpawnPoint);
        player.sendMessage("Set spawn to " + newSpawnPoint); //todo translation
    }
}
