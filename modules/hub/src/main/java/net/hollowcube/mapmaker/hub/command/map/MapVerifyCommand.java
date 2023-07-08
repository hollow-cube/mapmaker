package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.MASK_ID;
import static net.hollowcube.mapmaker.hub.command.ExtraArguments.MASK_SLOT;

public class MapVerifyCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT);

    public MapVerifyCommand() {
        super("verify");

        addSyntax(wrap(this::verifyMap), mapArg);
    }

    private void verifyMap(@NotNull Player player, @NotNull CommandContext context) {
        // todo: implement me
        player.sendMessage("verifyMap");
    }

}
