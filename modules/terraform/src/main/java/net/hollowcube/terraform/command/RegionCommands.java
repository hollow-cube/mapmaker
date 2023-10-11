package net.hollowcube.terraform.command;

import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.command.helper.ExtraArguments;
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
import org.jetbrains.annotations.Nullable;

public final class RegionCommands {
    private RegionCommands() {
    }

    public static final class Set extends Command {
        private final Argument<Block> patternArg = ArgumentType.BlockState("pattern"); //todo replace with pattern system
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Set(@Nullable CommandCondition condition) {
            super("set", "tf:set");
            setCondition(condition);

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage blah blah")); //todo
            addSyntax(this::handleSetRegionToPattern, patternArg);
            addSyntax(this::handleSetRegionToPattern, patternArg, selectionArg);
        }

        private void handleSetRegionToPattern(CommandSender sender, CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            // Determine the target selection
            var session = LocalSession.forPlayer(player);
            Selection selection;
            if (context.has(selectionArg)) {
                selection = session.selection(context.get(selectionArg));
            } else {
                selection = session.selection(Selection.DEFAULT);
            }

            var region = selection.region();
            if (region == null) {
                player.sendMessage(Component.translatable("command.terraform.set.no_selection"));
                return;
            }

            var pattern = context.get(patternArg);
            //todo validation if reqd later

            session.buildTask("set")
                    .metadata() //todo
                    .compute(world -> {
                        var buffer = BlockBuffer.builder(region.min(), region.max());
                        for (var pos : region) {
                            buffer.set(pos, pattern.stateId());
                        }
                        return buffer.build();
                    })
                    .submit();

//            session.action()
//                    .from(region)
//                    .set(pattern)
//                    .execute(summary -> {
//                        player.sendMessage(Component.translatable("command.terraform.set.success"));
//                    });
        }
    }

    public static final class Replace extends Command {
        private final Argument<Block> maskArg = ArgumentType.BlockState("mask"); //todo replace with mask system
        private final Argument<Block> patternArg = ArgumentType.BlockState("pattern"); //todo replace with pattern system
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Replace(@Nullable CommandCondition condition) {
            super("replace", "tf:replace");
            setCondition(condition);

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage blah blah")); //todo
            addSyntax(this::handleReplaceInRegion, maskArg, patternArg);
            addSyntax(this::handleReplaceInRegion, maskArg, patternArg, selectionArg);
        }

        private void handleReplaceInRegion(CommandSender sender, CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            // Determine the target selection
            var session = LocalSession.forPlayer(player);
            Selection selection;
            if (context.has(selectionArg)) {
                selection = session.selection(context.get(selectionArg));
            } else {
                selection = session.selection(Selection.DEFAULT);
            }

            var region = selection.region();
            if (region == null) {
                player.sendMessage(Component.translatable("command.terraform.no_selection"));
                return;
            }

            var pattern = context.get(patternArg);
            //todo validation if reqd later

            var mask = context.get(maskArg);

            session.action()
                    .from(region)
                    .set(pattern)
                    .matching((pos, block) -> block.id() == mask.id()) //todo this is always fuzzy, replace with proper api that handles fuzzy vs not.
                    .execute(summary -> {
                        player.sendMessage(Component.translatable("command.terraform.replace.success"));
                    });
        }
    }

}
