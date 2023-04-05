package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

public class AliasMapCommand extends Command {
    private MapStorage storage;

    public AliasMapCommand(MapStorage storage) {
        super("addAlias", "aa");
        this.storage = storage;
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /addAlias <id> <alias>"));
        addSyntax(this::parseArgument, ArgumentType.String("id"), ArgumentType.String("alias"));
    }

    private void parseArgument(@NotNull CommandSender sender, @NotNull CommandContext context) {
        String id = context.get("id");
        String alias = ((String) context.get("alias")).toUpperCase();

        try {
            // Get map data from input id (could be mapId, publishedId, or aliasId)
            FutureResult<String> mapIdFuture;
            if (id.length() > 16)
                mapIdFuture = FutureResult.of(id);
            else if (MapData.isValidAlias(id))
                mapIdFuture = storage.lookupAliasId(id).wrapErr("No such map with alias");
            else
                mapIdFuture = storage.lookupPublishedId(id).wrapErr("No such map with published id");

            mapIdFuture.flatMap(mapId -> {
                try {
                    MapData map = storage.getMapById(mapId).get();
                    String oldAlias = map.getDisplayedId();

                    // Set the map data new alias (and check valid)
                    var success = map.setAliasId(alias);
                    if (!success) {
                        sender.sendMessage("Invalid map alias.");
                        return null;
                    }

                    storage.lookupAliasId(alias).thenErr(err -> {
                        if (err.is(MapStorage.ERR_NOT_FOUND)) {
                            storage.updateMap(map);
                            sender.sendMessage(String.format("Changed map %s alias to %s", oldAlias, alias));
                        } else {
                            sender.sendMessage("Map with that alias already exists.");
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("Failed to update map with alias.");
        }
    }
}
