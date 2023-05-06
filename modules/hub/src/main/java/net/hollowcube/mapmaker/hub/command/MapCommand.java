package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLoop;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MapCommand extends BaseHubCommand {
    private static final Logger logger = LoggerFactory.getLogger(MapCommand.class);

    private final HubServer server;
    private final Handler handler;

    public MapCommand(@NotNull HubServer server, @NotNull Handler handler) {
        super("map", "m");
        this.server = server;
        this.handler = handler;

        // Playing
        addSubcommand(new InfoCommand());
        addSubcommand(new PlayCommand());

        // Building
        addSubcommand(new CreateCommand());
        addSubcommand(new EditCommand());
        addSubcommand(new PublishCommand());
        addSubcommand(new DeleteCommand());

        // Admin
        addSubcommand(new MapAdminCommand(server, handler));

        setDefaultExecutor(generateUsage());
    }

    public class InfoCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public InfoCommand() {
            super("info", "i");

            addSyntax(this::showMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void showMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                // In the future this could support console sending if we needed (eg specify a player)
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var longIdOrSlot = context.get(longIdOrSlotArg);
            var mapId = parseMapFromLongIdOrSlot(player, longIdOrSlot);
            if (mapId == null) return;

            var playerId = PlayerData.fromPlayer(player).getId();
            //todo
//            server.mapStorage().getMapById(mapId)
//                    .flatMap(map -> FutureResult.wrap(server.mapPermissions().checkPermission(map.getId(), playerId, MapData.Permission.READ))
//                            .map(unused -> map))
//                    .then(map -> {
//                        LanguageProvider.createMultiTranslatable("command.map.info.info",
//                                Component.text(map.getId()).clickEvent(ClickEvent.copyToClipboard(map.getId())).hoverEvent(HoverEvent.showText(Component.text("Click to copy"))),
//                                Component.text(map.getName()))
//                                .forEach(sender::sendMessage); //todo other info bits
//                    })
//                    .thenErr(err -> {
//                        if (err.is(MapStorage.ERR_NOT_FOUND) || err.is(MapPermissionManagerSpiceDB.ERR_NO_PERMISSION)) {
//                            sender.sendMessage(Component.translatable("command.map.info.not_found"));
//                            return;
//                        }
//
//                        logger.error("Failed to get map info for {}: {}", mapId, err);
//                        sender.sendMessage(Component.translatable("command.generic.unknown_error", Component.text(err.message())));
//                    });
        }
    }

    public class PlayCommand extends Command {
        private final Argument<String> shortOrLongIdArg = ArgumentType.String("map");

        public PlayCommand() {
            super("play");

            addSyntax(this::playMapWithId, shortOrLongIdArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /map play [mapId]"));
        }

        public @Blocking void playMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var shortOrLongId = context.get(shortOrLongIdArg);
            String mapId;
            if (shortOrLongId.length() < 36) {
                // Assume short ID
                mapId = server.mapStorage().lookupShortId(shortOrLongId);
            } else {
                // Long ID
                mapId = shortOrLongId;
            }

            try {
                handler.playMap(player, mapId);
            } catch (MapStorage.NotFoundError e) {
                sender.sendMessage(Component.translatable("command.map.generic.not_found", Component.text(shortOrLongId)));
            } catch (Exception e) {
                logger.error("failed to play map {}", shortOrLongId, e);
                sender.sendMessage(Component.translatable("command.generic.unknown_error", Component.text(e.getMessage())));
            }
        }
    }

    public class CreateCommand extends Command {
        private final Argument<Integer> mapSlot = ExtraArguments.MapSlot(true);
        private final ArgumentLoop<CommandContext> createMapOptions = ArgumentType.Loop(
                "options",
                ArgumentType.Group(
                        "nameGroup",
                        ArgumentType.Literal("name"),
                        ArgumentType.String("mapName")
                )
                //todo presets or other options
        );

        public CreateCommand() {
            super("create");

            addSyntax(this::createMapInGui, mapSlot);
            addSyntax(this::createMapWithDefault, mapSlot, ArgumentType.Literal("default"));
            addSyntax(this::createMapWithOptions, mapSlot, createMapOptions);

            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void createMapInGui(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                // In the future this could support console sending if we needed (eg specify a player)
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var slot = context.get(mapSlot);

            server.newOpenGUI(player, ctx -> {
                var view = new CreateMaps(ctx);
                view.createMapInSlot(slot);
                return view;
            });
        }

        public void createMapWithDefault(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                // In the future this could support console sending if we needed (eg specify a player)
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var slot = context.get(mapSlot);
            var protoMap = new MapData();

            handleCreateMap(player, slot, protoMap);
        }

        public void createMapWithOptions(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                // In the future this could support console sending if we needed (eg specify a player)
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var slot = context.get(mapSlot);
            var protoMap = new MapData();

            // Parse map options
            var options = context.get(createMapOptions);
            for (var option : options) {
                switch (option.getCommandName()) {
                    // Issues with name are caught later.
                    case "name" -> protoMap.setName(option.get("mapName"));
                    //todo others like preset
                }
            }

            handleCreateMap(player, slot, protoMap);
        }

        private void handleCreateMap(@NotNull Player player, int slot, @NotNull MapData protoMap) {
            var playerData = PlayerData.fromPlayer(player);
            protoMap.setOwner(playerData.getId());

            try {
                var map = handler.createMapForPlayerInSlot(playerData, protoMap, slot);
                LanguageProvider.createMultiTranslatable("command.map.create.success",
                        Component.text(slot), Component.text(map.getName()))
                        .forEach(player::sendMessage);
            } catch (Handler.MapSlotLockedError e) {
                player.sendMessage(Component.translatable("command.map.create.slot_locked"));
            } catch (Handler.MapSlotInUseError e) {
                player.sendMessage(Component.translatable("command.map.create.slot_in_use"));
            } catch (Handler.InvalidMapNameError e) {
                player.sendMessage(Component.translatable("command.map.create.invalid_name",
                        Component.text(protoMap.getName())));
            } catch (Exception e) {
                player.sendMessage(Component.translatable("command.generic.unknown_error"));
                logger.error("failed to create map for {} in slot {}: {}", playerData.getId(), slot, e.getMessage());
            }
        }
    }

    public class EditCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public EditCommand() {
            super("edit");

            addSyntax(this::editMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void editMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var longIdOrSlot = context.get(longIdOrSlotArg);
            var mapId = parseMapFromLongIdOrSlot(player, longIdOrSlot);
            if (mapId == null) return;

            try {
                handler.editMap(player, mapId);
            } catch (MapStorage.NotFoundError e) {
                sender.sendMessage(Component.translatable("command.map.generic.not_found", Component.text(longIdOrSlot)));
            } catch (Exception e) {
                logger.error("failed to edit map {}", longIdOrSlot, e);
                sender.sendMessage(Component.translatable("command.generic.unknown_error", Component.text(e.getMessage())));
            }
        }
    }

    public class PublishCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public PublishCommand() {
            super("publish");

            addSyntax(this::publishMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void publishMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var longIdOrSlot = context.get(longIdOrSlotArg);
            var mapId = parseMapFromLongIdOrSlot(player, longIdOrSlot);
            if (mapId == null) return;

            try {
                var map = handler.publishMap(PlayerData.fromPlayer(player).getId(), mapId);
                LanguageProvider.createMultiTranslatable("command.map.publish.success", Component.text(map.getId()), Component.text(map.getName()))
                        .forEach(player::sendMessage);
            } catch (MapStorage.NotFoundError e) {
                player.sendMessage(Component.translatable("command.map.generic.not_found", Component.text(mapId)));
            } catch (Exception e) {
                player.sendMessage(Component.translatable("command.generic.unknown_error"));
                logger.error("failed to publish map {}: {}", mapId, e.getMessage());
            }
        }
    }

    public class DeleteCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public DeleteCommand() {
            super("delete");

            addSyntax(this::deleteMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void deleteMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                // In the future this could support console sending if we needed (eg specify a player)
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            var longIdOrSlot = context.get(longIdOrSlotArg);
            var mapId = parseMapFromLongIdOrSlot(player, longIdOrSlot);
            if (mapId == null) return;

            try {
                var map = handler.deleteMap(mapId);
                LanguageProvider.createMultiTranslatable("command.map.delete.success", Component.text(map.getId()), Component.text(map.getName()))
                        .forEach(player::sendMessage);
            } catch (MapStorage.NotFoundError e) {
                player.sendMessage(Component.translatable("command.map.delete.not_found", Component.text(mapId)));
            } catch (Exception e) {
                player.sendMessage(Component.translatable("command.generic.unknown_error"));
                logger.error("failed to delete map {}: {}", mapId, e.getMessage());
            }
        }
    }

    private @NotNull CommandExecutor generateUsage() {
        List<Component> messages = new ArrayList<>();

        //todo generate usage. Move this to a BaseCommand in common and give it some fields like description
        // which can be set and used in here.
        messages.add(Component.text("usage todo"));

        return (sender, context) -> messages.forEach(sender::sendMessage);
    }

    private @Nullable String parseMapFromLongIdOrSlot(@NotNull Player player, @NotNull String longIdOrSlot) {
        String mapId = null;

        // Try to parse as slot
        try {
            var slot = Integer.parseInt(longIdOrSlot) - 1;

            var playerData = PlayerData.fromPlayer(player);
            if (playerData.getSlotState(slot) != PlayerData.SLOT_STATE_IN_USE) {
                if (slot < playerData.getUnlockedMapSlots()) {
                    player.sendMessage(Component.translatable("command.map.delete.slot_locked", Component.text(slot + 1)));
                } else {
                    player.sendMessage(Component.translatable("command.map.delete.slot_not_in_use", Component.text(slot + 1)));
                }
                return null;
            }

            mapId = playerData.getMapSlot(slot);
        } catch (NumberFormatException ignored) {
            // It's not a number, which is ok it might be a UUID
        }

        // Try to parse into uuid
        if (mapId == null) {
            try {
                mapId = UUID.fromString(longIdOrSlot).toString();
            } catch (IllegalArgumentException ignored) {
                player.sendMessage(Component.translatable("command.map.delete.invalid_id", Component.text(longIdOrSlot)));
                return null;
            }
        }

        return mapId;
    }
}
