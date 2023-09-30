package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.util.HubMessages;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.MinecraftServer;
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
        addSyntax(wrap(this::importMap), playerArg, legacyMapIdArg);
    }

    private void importMap(@NotNull Player player, @NotNull CommandContext context) {
        var authorizer = player.getUuid().toString();

        var mapOwner = context.getOrDefault(playerArg, authorizer);
        var legacyMapId = context.get(legacyMapIdArg);

        try {
            var mapData = mapService.importLegacyMap(authorizer, mapOwner, legacyMapId);
            player.sendMessage(HubMessages.COMMAND_MAP_LEGACY_IMPORT_SUCCESS
                    .with(mapData.settings().getNameComponent(), mapData.slot()));

        } catch (MapService.NotFoundError ignored) {
            player.sendMessage(HubMessages.COMMAND_MAP_LEGACY_IMPORT_NOT_FOUND
                    .with(legacyMapId));
        } catch (MapService.NoPermissionError ignored) {
            player.sendMessage(HubMessages.COMMAND_MAP_LEGACY_IMPORT_NO_PERMISSION
                    .with(legacyMapId));
        } catch (Exception e) {
            player.sendMessage(HubMessages.COMMAND_MAP_LEGACY_IMPORT_UNKNOWN_ERROR
                    .with(legacyMapId));
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

}
