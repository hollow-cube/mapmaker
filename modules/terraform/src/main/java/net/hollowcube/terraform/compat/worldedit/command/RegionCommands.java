package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RegionCommands {
    private RegionCommands() {
    }

    public static final class Set extends Command {
        Argument<Block> blockArg = ArgumentType.BlockState("block");

        public Set(@Nullable CommandCondition commandCondition) {
            super("/set");
            setCondition(commandCondition);

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: //set <block>"));
            addSyntax(this::setWithBlock, blockArg);
        }

        public void setWithBlock(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            //todo need to support other patterns
            var block = context.get(blockArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Component.translatable("command.worldedit.no_selection"));
                return;
            }

            session.action()
                    .from(region)
                    .set(block)
                    .execute(summary -> {
                        player.sendMessage(Component.translatable("command.worldedit.set.success"));
                    });
        }
    }
}
