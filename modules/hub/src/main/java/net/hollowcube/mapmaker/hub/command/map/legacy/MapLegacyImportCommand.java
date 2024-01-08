package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.hub.util.HubMessages;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapLegacyImportCommand extends CommandDsl {
    private final Argument<String> playerArg = Argument.Word("player");
    private final Argument<String> mapIdArg = Argument.Word("id");
//            .map((sender, raw) -> {
//                if (raw.length() > 5)
//                    return new Argument.ParseFailure<>();
//                return new Argument.ParseSuccess<>(raw);
//            });

    private final MapService mapService;

    public MapLegacyImportCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("import");
        this.mapService = mapService;

        var importAnyPerm = permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN);

        addSyntax(playerOnly(this::importLegacyMap), mapIdArg);
//        addSyntax(importAnyPerm, playerOnly(this::importLegacyMap), playerArg, mapIdArg);
    }

    private void importLegacyMap(@NotNull Player player, @NotNull CommandContext context) {
        var authorizer = player.getUuid().toString();

        var mapOwner = context.has(playerArg) ? context.get(playerArg) : authorizer;
        var legacyMapId = context.get(mapIdArg);

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
