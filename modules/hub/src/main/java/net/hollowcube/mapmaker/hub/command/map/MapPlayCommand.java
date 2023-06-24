package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapPlayCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PERSONAL_WORLD | MASK_PUBLISHED_ID);

    public MapPlayCommand() {
        super("play");

        addSyntax(wrap(this::playMap), mapArg);
    }

    private void playMap(@NotNull Player player, @NotNull CommandContext context) {
        //todo personal world should be explicitly handled to return an error message

        // todo: implement me
        player.sendMessage("playMap");
    }

}
