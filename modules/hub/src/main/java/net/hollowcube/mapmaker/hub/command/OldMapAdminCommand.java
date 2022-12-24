package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.handler.MapAdminHandler;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OldMapAdminCommand extends Command {
    //todo how are we going to do completion on command arguments?
    //     - Querying mongo every time is almost certainly too slow, will need some kind of caching mechanism and maybe local filtering?
    //     - Or could use a service like meillisearch, but its more to setup and manage.


    /*

    ADMIN - TODO ALL SHOULD HAVE GUI OPTIONALLY
    /map admin -> usage
    /map admin list -> list all maps, paginated
    /map admin list <player> -> list all maps by player, paginated
    /map admin info <map id> -> info about a map, no matter the owner
    /map admin delete <map id> -> deletes a map, no matter the owner

     */

    private final MapAdminHandler handler;

    public OldMapAdminCommand(@NotNull MapStorage storage) {
        super("admin");
        this.handler = new MapAdminHandler(storage);

        setDefaultExecutor(this::showUsage);

        addSubcommand(new List());
        addSubcommand(new Info());
        addSubcommand(new Delete());
    }

    private void showUsage(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("Usage: /map admin <subcommand>");
    }

    private class List extends Command {
        private final Argument<String> playerArg = ArgumentType.String("player");

        public List() {
            super("list");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map admin list [player]"));

            addSyntax(this::listAllMaps);
            addSyntax(this::listMapsByPlayer, playerArg);
        }

        private void listAllMaps(@NotNull CommandSender sender, @NotNull CommandContext context) {
            sender.sendMessage("not implemented");
        }

        private void listMapsByPlayer(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.generic.player_only"));
                return;
            }

            var playerId = context.get(playerArg);
            if (playerId == null) {
                sender.sendMessage(Component.translatable("command.generic.sanity_check_failed"));
                return;
            }

            handler.showMapList(player, playerId);
        }

    }

    private class Info extends Command {
        private final Argument<String> mapIdArg = ArgumentType.String("map_id");

        public Info() {
            super("info");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map admin info <map id>"));

            addSyntax(this::showInfoById, mapIdArg);
        }

        private void showInfoById(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.generic.player_only"));
                return;
            }

            var mapId = context.get(mapIdArg);
            if (mapId == null) {
                sender.sendMessage(Component.translatable("command.generic.sanity_check_failed"));
                return;
            }

            handler.showMapInfoById(player, mapId);
        }
    }

    private class Delete extends Command {
        private final Argument<String> mapIdArg = ArgumentType.String("map_id");

        public Delete() {
            super("delete");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map admin delete <map id>"));

            addSyntax(this::deleteMapById, mapIdArg);
        }

        private void deleteMapById(@NotNull CommandSender sender, @NotNull CommandContext context) {
            sender.sendMessage("not implemented");
        }
    }
}
