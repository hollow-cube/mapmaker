package net.hollowcube.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class SetSpawnCommand extends CommandDsl {
    private final Argument2<Point> coordArgument = Argument2.RelativeVec3("position");
    private final Argument2<Float> yawArgument = Argument2.Float("yaw");
    private final Argument2<Float> pitchArgument = Argument2.Float("pitch");

    @Inject
    public SetSpawnCommand() {
        super("setspawn");
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
        if (!player.getInstance().getWorldBorder().isInside(newSpawnPoint)) {
            player.sendMessage(Component.translatable("command.set_spawn.out_of_world"));
            return;
        }

        var map = MapWorld.forPlayer(player).map();
        map.settings().setSpawnPoint(newSpawnPoint);
        player.sendMessage(MapMessages.COMMAND_SETSPAWN_SUCCESS.with(
                Component.text(newSpawnPoint.blockX()).hoverEvent(Component.text(newSpawnPoint.x(), NamedTextColor.WHITE)),
                Component.text(newSpawnPoint.blockY()).hoverEvent(Component.text(newSpawnPoint.y(), NamedTextColor.WHITE)),
                Component.text(newSpawnPoint.blockZ()).hoverEvent(Component.text(newSpawnPoint.z(), NamedTextColor.WHITE)),
                Component.text(Math.floor(newSpawnPoint.pitch())).hoverEvent(Component.text(newSpawnPoint.pitch(), NamedTextColor.WHITE)),
                Component.text(Math.floor(newSpawnPoint.yaw())).hoverEvent(Component.text(newSpawnPoint.yaw(), NamedTextColor.WHITE))
        ));
    }
}
