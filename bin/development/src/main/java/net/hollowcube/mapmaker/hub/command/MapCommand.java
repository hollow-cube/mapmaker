package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.MapHandle;
import net.hollowcube.mapmaker.hub.handler.MapHandler;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.util.FutureUtil;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapCommand extends Command {
    private final MapHandler handler;

    public MapCommand(MapHandler handler) {
        super("map");
        this.handler = handler;

        addSubcommand(new Create());
        addSubcommand(new Info());
        addSubcommand(new Edit());
    }

    private class Create extends Command {
        private final Argument<MapData.Type> typeArg = ArgumentType.Enum("type", MapData.Type.class);
        private final Argument<String> nameArg = ArgumentType.String("name");

        public Create() {
            super("create");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map create <type> <name>"));

            addSyntax(this::createWithTypeAndName, typeArg, nameArg);
        }

        private void createWithTypeAndName(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var type = context.get(typeArg);
            var name = context.get(nameArg);

            handler.createMap(player, type, name)
                    .exceptionally(FutureUtil::handleException);
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

            //todo handler should handle this, and handler should have an interface to create maps
            var mapId = context.get(idArg);
            handler.editMap(mapId, player)
                    .exceptionally(FutureUtil::handleException);
        }
    }
}
