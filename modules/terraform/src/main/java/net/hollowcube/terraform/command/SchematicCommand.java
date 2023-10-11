package net.hollowcube.terraform.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.condition.CommandCondition;
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
            sender.sendMessage("implement me");
        }
    }

    public static final class Load extends Command {

        public Load() {
            super("load");
        }

        private void handleLoadSchem(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            player.sendMessage("implement me");
        }
    }

}
