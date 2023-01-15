package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.history.MultiBlockChange;
import net.hollowcube.terraform.session.Session;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.BatchOption;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class RegionCommands {

    public RegionCommands(CommandManager commands, @Nullable CommandCondition commandCondition) {
        commands.register(new SetCommand(commandCondition));
    }


    public static class SetCommand extends Command {
        Argument<Block> blockArg = ArgumentType.BlockState("block");

        public SetCommand(@Nullable CommandCondition commandCondition) {
            super("/set");
            setCondition(commandCondition);

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: //set <block>"));
            addSyntax(this::setWithBlock, blockArg);
        }

        public void setWithBlock(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player))
                throw new UnsupportedOperationException("only implemented for players");

            var session = Session.forPlayer(player);
            var region = session.getRegionSelector(player.getInstance()).getRegion();

            if (region == null) {
                player.sendMessage("Make a region selection first.");
                return;
            }

            var block = context.get(blockArg);
            var batch = new AbsoluteBlockBatch(new BatchOption().setCalculateInverse(true));
            for (var pos : region) {
                batch.setBlock(pos, block);
            }

            var instance = player.getInstance();
            var inverse = new AtomicReference<AbsoluteBlockBatch>();
            inverse.set(batch.apply(instance, () -> {
                session.remember(new MultiBlockChange(batch, inverse.get()));
                sender.sendMessage("Done!");
            }));
        }
    }
}
