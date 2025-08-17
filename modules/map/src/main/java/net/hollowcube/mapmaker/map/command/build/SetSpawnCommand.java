package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.vnext.MapChangeSpawnPointEvent;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

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

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleSetSpawnToPlayer));
        addSyntax(playerOnly(this::handleSetSpawnToCoords), coordArgument, yawArgument, pitchArgument);
    }

    private void handleSetSpawnToPlayer(@NotNull Player player, @NotNull CommandContext context) {
        updateMapPos(player, player.getPosition());
    }

    private void handleSetSpawnToCoords(@NotNull Player player, @NotNull CommandContext context) {
        updateMapPos(player, new Pos(context.get(coordArgument), context.get(yawArgument), context.get(pitchArgument)));
    }

    private void updateMapPos(@NotNull Player player, @NotNull Pos newSpawnPoint) {
        if (!CoordinateUtil.inBorder(player.getInstance().getWorldBorder(), newSpawnPoint, 2)) {
            player.sendMessage(Component.translatable("command.set_spawn.out_of_world"));
            return;
        }

        var world = MapWorld.forPlayer(player);
        world.callEvent(new MapChangeSpawnPointEvent(world, newSpawnPoint));
        world.map().settings().setSpawnPoint(newSpawnPoint);

        player.sendMessage(MapMessages.COMMAND_SETSPAWN_SUCCESS.with(CoordinateUtil.asTranslationArgs(newSpawnPoint)));
    }
}
