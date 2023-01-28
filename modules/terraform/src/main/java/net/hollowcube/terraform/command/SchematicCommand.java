package net.hollowcube.terraform.command;

import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.util.TempSchematicStuff;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SchematicCommand extends Command {

    public SchematicCommand(@Nullable CommandCondition condition) {
        super("schematic", "schem");
        setCondition(condition);

        addSubcommand(new List());
        addSubcommand(new Load());
    }

    public static final class List extends Command {
        public List() {
            super("list");

            setDefaultExecutor(this::handleSchemList);
        }

        private void handleSchemList(@NotNull CommandSender sender, @NotNull CommandContext context) {
            sender.sendMessage("Schematics:");
            TempSchematicStuff.getSchematics().forEach(sender::sendMessage);
        }
    }

    public static final class Load extends Command {
        private final Argument<String> schemNameArg = ArgumentType.String("schematic")
                .setSuggestionCallback((sender, context, suggestions) -> {
                    TempSchematicStuff.getSchematics().forEach(name -> {
                        if (name.startsWith(context.getInput())) {
                            suggestions.addEntry(new SuggestionEntry(name));
                        }
                    });
                });

        public Load() {
            super("load");

            addSyntax(this::handleLoadSchem, schemNameArg);
        }

        private void handleLoadSchem(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            var playerSession = PlayerSession.forPlayer(player);
            var schemName = context.get(schemNameArg);
            var schem = TempSchematicStuff.load(schemName);
            if (schem == null) {
                player.sendMessage(Component.translatable("command.terraform.schematic.not_found", Component.text(schemName)));
                return;
            }

            playerSession.setClipboard(schem);
        }
    }

}
