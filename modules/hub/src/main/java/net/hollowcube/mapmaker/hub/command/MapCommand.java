package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.handler.MapHandler;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.result.Result;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapCommand extends BaseHubCommand {
    private static final Logger logger = LoggerFactory.getLogger(MapCommand.class);

    /*
    PLAYERS
    /map -> usage
    /map create <name> [slot] -> creates a map under the current player (if slot not specified it will choose the first available)
    /map delete <name> -> deletes a map for the current player
    /map edit <name> -> edits a map for the current player (by name)
    /map list -> lists all maps for the current player
     */

    private final MapHandler handler;

    public MapCommand(MapHandler handler) {
        super("map");
        this.handler = handler;

        addSubcommand(new Create());
        addSubcommand(new Info());
        addSubcommand(new Edit());
        addSubcommand(new Play());

        addSubcommand(new MapAdminCommand(handler.storage()));
    }

    private class Create extends Command {
        private final Argument<String> nameArg = ArgumentType.String("name");
        private final Argument<Integer> slotArg = ArgumentType.Integer("slot").min(1).max(PlayerData.MAX_MAP_SLOTS + 1);

        public Create() {
            super("create");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map create <type> <name> <slot>"));

            addSyntax(this::createWithTypeNameSlot, nameArg, slotArg);
        }

        private void createWithTypeNameSlot(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var name = context.get(nameArg);
            var slot = context.get(slotArg);

            handler.createMap(player, name, slot)
                    .then(map -> {
                        LanguageProvider.createMultiTranslatable("command.map.create.success",
                                        Component.text(map.getName())
                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                                                .clickEvent(ClickEvent.copyToClipboard(map.getId())),
                                        Component.text(slot + 1))
                                .forEach(player::sendMessage);
                        logger.info("{} created map {} in slot {}", player.getUsername(), map.getName(), slot);
                    })
                    .thenErr(err -> {
                        if (err.is(MapHandler.ERR_SLOT_IN_USE)) {
                            LanguageProvider.createMultiTranslatable("command.map.create.slot_in_use",
                                    Component.text(slot + 1)).forEach(player::sendMessage);
                        } else if (err.is(MapHandler.ERR_DUPLICATE_NAME)) {
                            LanguageProvider.createMultiTranslatable("command.map.create.name_in_use",
                                    Component.text(name)).forEach(player::sendMessage);
                        } else {
                            LanguageProvider.createMultiTranslatable("command.generic.unknown_error",
                                    Component.text(err.toString())).forEach(player::sendMessage);
                            logger.error("Error creating map: {}", err);
                        }
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
