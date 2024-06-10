package net.hollowcube.mapmaker.command.map.legacy;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapLegacyImportCommand extends CommandDsl {
    private final Argument<String> playerArg = Argument.Word("player")
            .description("The UUID of the player to list the legacy maps of");
    private final Argument<String> mapIdArg = Argument.Word("id")
            .map((sender, raw) -> {
                if (raw.length() > 7)
                    return new ParseResult.Failure<>(-1);
                return new ParseResult.Success<>(raw);
            })
            .description("The legacy map ID of the map to import");

    private final MapService mapService;
    private final CommandCondition importAnyPerm;

    public MapLegacyImportCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("import");
        this.mapService = mapService;

        importAnyPerm = permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN);
    }

    @Override
    public void build(@NotNull CommandBuilder builder) {
        builder.description("Import a map from Omega Parkour or Tapple");
        builder.examples("/map legacy import 12345");
        builder.child(mapIdArg, importSingle -> importSingle
                        .executes(playerOnly(this::importLegacyMap)))
                .child(playerArg, importOther -> importOther
                        .condition(importAnyPerm)
                        .executes(playerOnly(this::importLegacyMap), mapIdArg));
    }

    private void importLegacyMap(@NotNull Player player, @NotNull CommandContext context) {
        var authorizer = player.getUuid().toString();

        var mapOwner = context.has(playerArg) ? context.get(playerArg) : authorizer;
        var legacyMapId = context.get(mapIdArg);

        try {
            var mapData = mapService.importLegacyMap(authorizer, mapOwner, legacyMapId);
            player.sendMessage(GenericMessages.COMMAND_MAP_LEGACY_IMPORT_SUCCESS
                    .with(mapData.settings().getNameComponent(), mapData.slot() + 1));

            // We were successful in importing so we should update the players map list locally.
            // It is likely we have not received the updated slot list yet.
            var mapPlayerData = MapPlayerData.fromPlayer(player);
            mapPlayerData.mapSlots()[mapData.slot()] = mapData.id();
        } catch (MapService.NotFoundError ignored) {
            player.sendMessage(GenericMessages.COMMAND_MAP_LEGACY_IMPORT_NOT_FOUND
                    .with(legacyMapId));
        } catch (MapService.NoPermissionError ignored) {
            player.sendMessage(GenericMessages.COMMAND_MAP_LEGACY_IMPORT_NO_PERMISSION
                    .with(legacyMapId));
        } catch (MapService.SlotInUseError ignored) {
            player.sendMessage("no available slots");
        } catch (Exception e) {
            player.sendMessage(GenericMessages.COMMAND_MAP_LEGACY_IMPORT_UNKNOWN_ERROR
                    .with(legacyMapId));
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }
}
