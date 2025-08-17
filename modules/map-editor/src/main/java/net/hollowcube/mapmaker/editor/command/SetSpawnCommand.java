package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.kyori.adventure.text.Component.translatable;

public class SetSpawnCommand extends CommandDsl {
    private final Argument<Point> coordArgument = Argument.RelativeVec3("position")
            .description("The position to set the spawn point to");
    private final Argument<Float> yawArgument = Argument.Float("yaw")
            .description("The yaw to set the spawn point to");
    private final Argument<Float> pitchArgument = Argument.Float("pitch")
            .description("The pitch to set the spawn point to");

    public SetSpawnCommand() {
        super("setspawn", "setstart");

        description = "Sets the spawn point of your map to where you’re standing or to the specified coordinates";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleSetSpawnToPlayer));
        addSyntax(playerOnly(this::handleSetSpawnToCoords), coordArgument, yawArgument, pitchArgument);
    }

    private void handleSetSpawnToPlayer(Player player, CommandContext context) {
        updateMapPos(player, player.getPosition());
    }

    private void handleSetSpawnToCoords(Player player, CommandContext context) {
        updateMapPos(player, new Pos(context.get(coordArgument), context.get(yawArgument), context.get(pitchArgument)));
    }

    private void updateMapPos(Player player, Pos newSpawnPoint) {
        if (!CoordinateUtil.inBorder(player.getInstance().getWorldBorder(), newSpawnPoint, 2)) {
            player.sendMessage(translatable("command.set_spawn.out_of_world"));
            return;
        }

        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        world.setSpawnPoint(newSpawnPoint);
        player.sendMessage(translatable("command.set_spawn.success", CoordinateUtil.asTranslationArgs(newSpawnPoint)));
    }
}
