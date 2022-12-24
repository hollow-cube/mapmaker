package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.HubServer;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLoop;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MapCommand extends BaseHubCommand {
    private final HubServer server;

    public MapCommand(@NotNull HubServer server) {
        super("map", "m");
        this.server = server;

        // Playing
        addSubcommand(new PlayCommand());

        // Building
        addSubcommand(new CreateCommand());
        addSubcommand(new EditCommand());
        addSubcommand(new PublishCommand());
        addSubcommand(new DeleteCommand());

        setDefaultExecutor(generateUsage());
    }

    public class PlayCommand extends Command {
        private final Argument<String> shortOrLongIdArg = ArgumentType.String("map");

        public PlayCommand() {
            super("play");

            addSyntax(this::playMapWithId, shortOrLongIdArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void playMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var shortOrLongId = context.get(shortOrLongIdArg);
            sender.sendMessage("TODO - play map " + shortOrLongId);
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
            var slot = context.get(mapSlot);
            sender.sendMessage("create map gui " + slot);
        }

        public void createMapWithDefault(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var slot = context.get(mapSlot);
            sender.sendMessage("create map default " + slot);
        }

        public void createMapWithOptions(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var slot = context.get(mapSlot);
            sender.sendMessage("create map options " + slot);
        }
    }

    public class EditCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public EditCommand() {
            super("edit");

            //todo its possible it will be easier to use a separate syntax for slot vs long id
            addSyntax(this::editMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void editMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var longIdOrSlot = context.get(longIdOrSlotArg);
            sender.sendMessage("TODO - edit map " + longIdOrSlot);
        }
    }

    public class PublishCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public PublishCommand() {
            super("publish");

            //todo its possible it will be easier to use a separate syntax for slot vs long id
            addSyntax(this::publishMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void publishMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var longIdOrSlot = context.get(longIdOrSlotArg);
            sender.sendMessage("TODO - publish map " + longIdOrSlot);
        }
    }

    public class DeleteCommand extends Command {
        private final Argument<String> longIdOrSlotArg = ArgumentType.String("map");

        public DeleteCommand() {
            super("delete");

            //todo its possible it will be easier to use a separate syntax for slot vs long id
            addSyntax(this::deleteMapWithId, longIdOrSlotArg);
            setDefaultExecutor((sender, context) -> sender.sendMessage("todo"));
        }

        public void deleteMapWithId(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var longIdOrSlot = context.get(longIdOrSlotArg);
            sender.sendMessage("TODO - publish map " + longIdOrSlot);
        }
    }

    private @NotNull CommandExecutor generateUsage() {
        List<Component> messages = new ArrayList<>();

        //todo generate usage. Move this to a BaseCommand in common and give it some fields like description
        // which can be set and used in here.
        messages.add(Component.text("usage todo"));

        return (sender, context) -> messages.forEach(sender::sendMessage);
    }
}
