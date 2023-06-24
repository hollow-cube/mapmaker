package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapCopyCommand extends BaseHubCommand {
    private final Argument<MapData> fromArg = ExtraArguments.Map("from", MASK_ID | MASK_SLOT | MASK_PUBLISHED_ID);
    private final Argument<Integer> toArg = ExtraArguments.MapSlot("to", true);

    public MapCopyCommand() {
        super("copy");

        addSyntax(wrap(this::copyMapToSlot), fromArg, toArg);
    }

    private void copyMapToSlot(@NotNull Player player, @NotNull CommandContext context) {
        // todo: implement me
        player.sendMessage("copyMapToSlot");
    }

}
