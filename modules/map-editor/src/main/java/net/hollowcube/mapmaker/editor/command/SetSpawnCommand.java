package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.CommonEditorActions;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

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
        CommonEditorActions.trySetSpawn(player, player.getPosition());
    }

    private void handleSetSpawnToCoords(Player player, CommandContext context) {
        CommonEditorActions.trySetSpawn(
            player,
            new Pos(context.get(coordArgument), context.get(yawArgument), context.get(pitchArgument))
        );
    }
}
