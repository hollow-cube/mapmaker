package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.model.PlayerData;
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

public class OldMapCommand extends BaseHubCommand {
    private static final Logger logger = LoggerFactory.getLogger(OldMapCommand.class);

    /*
     * TODOs for command rework:
     * - /map commands will probably be removed, or at least split up
     *
     *
     * COMMAND LIST (todo move to google doc or spreadsheet)
     * /play <shortOrLongId> - play a map given its short or long ID (alias for /map play)
     * /map play <shortOrLongId> - ...
     * /edit <longIdOrSlot> - edit a map given its long ID or slot number (alias for /map edit)
     * /map edit <longIdOrSlot> - ...
     * /publish <longIdOrSlot> - publish the map in the given slot or with the given ID (alias for /map publish)
     * /map publish <longIdOrSlot> - ...
     * /map create <slot> - opens the map create gui for the given slot
     * /map create <slot> default - creates the default map in the given slot
     * /map create <slot> name "abc" template parkour etc etc - creates a map with the given options (default cannot be used with any of those options)
     * /map delete <longIdOrSlot> - deletes the map in the given slot or with the given ID
     *
     * /map search - todo
     *
     *
     * ADMIN COMMAND LIST
     *
     * /map admin search - todo
     *
     *
     *
     */


    /*
    PLAYERS
    /map -> usage
    /map create <name> [slot] -> creates a map under the current player (if slot not specified it will choose the first available)
    /map delete <name> -> deletes a map for the current player
    /map edit <name> -> edits a map for the current player (by name)
    /map list -> lists all maps for the current player
     */

    private final Handler handler;

    public OldMapCommand(@NotNull HubServer server, Handler handler) {
        super("map");
        this.handler = handler;

        addSubcommand(new Create());
        addSubcommand(new Publish());
        addSubcommand(new Info());
        addSubcommand(new Edit());
        addSubcommand(new Play());

        addSubcommand(new OldMapAdminCommand(server.mapStorage()));
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
                        if (err.is(Handler.ERR_SLOT_IN_USE)) {
                            LanguageProvider.createMultiTranslatable("command.map.create.slot_in_use",
                                    Component.text(slot + 1)).forEach(player::sendMessage);
                        } else if (err.is(Handler.ERR_DUPLICATE_NAME)) {
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

    public class Publish extends Command {
        private final Argument<Integer> slotArg = ArgumentType.Integer("slot").min(1).max(PlayerData.MAX_MAP_SLOTS + 1);

        public Publish() {
            super("publish");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map publish <slot>"));

            addSyntax(this::publishWithSlot, slotArg);
        }

        private void publishWithSlot(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) return;

            var slot = context.get(slotArg);

            handler.publishMap(player, slot - 1)
                    .then(unused -> {
                        LanguageProvider.createMultiTranslatable("command.map.publish.success",
                                Component.text(slot)).forEach(player::sendMessage);
                        logger.info("{} published map in slot {}", player.getUsername(), slot);
                    })
                    .thenErr(err -> {
                        if (err.is(Handler.ERR_SLOT_NOT_IN_USE)) {
                            LanguageProvider.createMultiTranslatable("command.map.public.slot_not_in_use",
                                    Component.text(slot)).forEach(player::sendMessage);
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
            handler.editMap2(player, mapId)
                    .mapErr(err -> {
                        LOGGER.error("Failed to edit map: {}", err);
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
