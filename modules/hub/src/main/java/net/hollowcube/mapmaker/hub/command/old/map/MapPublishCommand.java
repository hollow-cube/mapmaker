package net.hollowcube.mapmaker.hub.command.old.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapPublishCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PERSONAL_WORLD | MASK_PUBLISHED_ID);

    public MapPublishCommand() {
        super("publish");

        addSyntax(wrap(this::publishMap), mapArg);

        //todo special error message for personal world USING ARGUMENT CALLBACKS
    }

    private void publishMap(@NotNull Player player, @NotNull CommandContext context) {
        // todo: implement me
        player.sendMessage("publishMap");
    }

}
