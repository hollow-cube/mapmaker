package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapLegacyImportCommand extends BaseHubCommand {
    private final Argument<String> playerArg = ArgumentType.String("player"); //todo support names too, and offline players -- this is a weird query tho need to get a list of all potential legacy players
    private final Argument<String> legacyMapIdArg = ArgumentType.String("id");

    private final MapService mapService;

    public MapLegacyImportCommand(@NotNull MapService mapService) {
        super("import");
        this.mapService = mapService;

        //todo usage

        addSyntax(wrap(this::importMap), legacyMapIdArg);
        addSyntax(wrap(this::importMap2), playerArg, legacyMapIdArg);
//        addSyntax(wrap(this::listMaps), playerArg);
    }

    private void importMap(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("H1");
    }

    private void importMap2(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("H2");
    }

}
