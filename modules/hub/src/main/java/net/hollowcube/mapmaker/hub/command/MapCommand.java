package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.handler.MapHandler;
import net.hollowcube.mapmaker.result.Result;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapCommand extends BaseHubCommand {
    private final MapHandler handler;

    public MapCommand(MapHandler handler) {
        super("map");
        this.handler = handler;

        addSubcommand(new Create());
        addSubcommand(new Info());
        addSubcommand(new Edit());
        addSubcommand(new Play());
    }

    private class Create extends Command {
        private final Argument<String> nameArg = ArgumentType.String("name");

        public Create() {
            super("create");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map create <type> <name>"));

            addSyntax(this::createWithTypeAndName, nameArg);
        }

        private void createWithTypeAndName(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var name = context.get(nameArg);

            handler.createMap(player, name)
                    .mapErr(err -> {
                        LOGGER.error("Failed to create map: {}", err);
                        return Result.error(err);
                    });
        }
    }

    public class Info extends Command {
        private final Argument<String> mapIdArg = ArgumentType.String("map-id");

        public Info() {
            super("info");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map info <map-id>"));

            addSyntax(this::infoWithMapId, mapIdArg);
        }

        private void infoWithMapId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var mapId = context.get(mapIdArg);
            handler.infoMap(mapId, player)
                    .mapErr(err -> {
                        LOGGER.error("Failed to create map: {}", err);
                        return Result.error(err);
                    });
        }
    }

    private class Edit extends Command {
        private final Argument<String> idArg = ArgumentType.String("map-id");

        public Edit() {
            super("edit");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map edit <map-id>"));

            addSyntax(this::editWithId, idArg);
        }

        private void editWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var mapId = context.get(idArg);
            handler.editMap(mapId, player)
                    .mapErr(err -> {
                        LOGGER.error("Failed to create map: {}", err);
                        return Result.error(err);
                    });
        }
    }

    private class Play extends Command {
        private final Argument<String> idArg = ArgumentType.String("map-id");

        public Play() {
            super("play");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map play <map-id>"));

            addSyntax(this::editWithId, idArg);
        }

        private void editWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var mapId = context.get(idArg);
            handler.playMap(mapId, player)
                    .mapErr(err -> {
                        LOGGER.error("Failed to create map: {}", err);
                        return Result.error(err);
                    });
        }
    }
}
