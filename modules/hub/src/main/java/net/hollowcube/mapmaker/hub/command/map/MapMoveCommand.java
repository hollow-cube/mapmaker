package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapMoveCommand extends BaseHubCommand {
    private final Argument<MapData> fromArg = ExtraArguments.Map("from", MASK_SLOT);
    private final Argument<Integer> toSlotArg = ExtraArguments.MapSlot("to", true);

    public MapMoveCommand() {
        super("move");

        addSyntax(wrap(this::moveMapToSlot), fromArg, toSlotArg);
    }

    private void moveMapToSlot(@NotNull Player player, @NotNull CommandContext context) {
        // todo: implement me
        player.sendMessage("moveMapToSlot");
    }

}
