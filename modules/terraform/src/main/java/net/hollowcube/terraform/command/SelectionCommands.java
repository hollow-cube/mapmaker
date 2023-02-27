package net.hollowcube.terraform.command;

import net.hollowcube.terraform.command.argument.ExtraArguments;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.location.RelativeVec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SelectionCommands {
    private SelectionCommands() {}

    public static final class Pos1 extends Command {
        private final Argument<RelativeVec> posArg = ArgumentType.RelativeVec3("pos");
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Pos1(@Nullable CommandCondition condition) {
            super("pos1", "tf:pos1");
            setCondition(condition);

            setDefaultExecutor(this::handleSelectionUpdate);
            addSyntax(this::handleSelectionUpdate, posArg);
            addSyntax(this::handleSelectionUpdate, posArg, selectionArg);
        }

        private void handleSelectionUpdate(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            // Determine the target position
            Point pos;
            if (context.has(posArg)) {
                pos = context.get(posArg).fromSender(sender);
            } else {
                pos = player.getPosition();
            }

            // Determine the target selection
            var session = LocalSession.forPlayer(player);
            Selection selection;
            if (context.has(selectionArg)) {
                selection = session.selection(context.get(selectionArg));
            } else {
                selection = session.selection(Selection.DEFAULT);
            }

            // Update the selection
            var changed = selection.selectPrimary(pos, true);
            if (!changed) {
                player.sendMessage(Component.translatable("command.terraform.pos1.already_set"));
            }
        }
    }

    public static final class Pos2 extends Command {
        private final Argument<RelativeVec> posArg = ArgumentType.RelativeVec3("pos");
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Pos2(@Nullable CommandCondition condition) {
            super("pos2", "tf:pos2");
            setCondition(condition);

            setDefaultExecutor(this::handleSelectionUpdate);
            addSyntax(this::handleSelectionUpdate, posArg);
            addSyntax(this::handleSelectionUpdate, posArg, selectionArg);
        }

        private void handleSelectionUpdate(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            // Determine the target position
            Point pos;
            if (context.has(posArg)) {
                pos = context.get(posArg).fromSender(sender);
            } else {
                pos = player.getPosition();
            }

            // Determine the target selection
            var session = LocalSession.forPlayer(player);
            Selection selection;
            if (context.has(selectionArg)) {
                selection = session.selection(context.get(selectionArg));
            } else {
                selection = session.selection(Selection.DEFAULT);
            }

            // Update the selection
            var changed = selection.selectSecondary(pos, true);
            if (!changed) {
                player.sendMessage(Component.translatable("command.terraform.pos2.already_set"));
            }
        }
    }

    public static final class HPos1 extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public HPos1(@Nullable CommandCondition condition) {
            super("hpos1", "tf:hpos1");
            setCondition(condition);

            setDefaultExecutor(this::handleSelectionUpdate);
            addSyntax(this::handleSelectionUpdate, selectionArg);
        }

        private void handleSelectionUpdate(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            // Determine the target position
            Point targetBlock;
            try {
                targetBlock = player.getTargetBlockPosition(512); //todo could be an option somewhere maybe
            } catch (NullPointerException e) {
                if (!e.getMessage().contains("Unloaded chunk"))
                    throw new RuntimeException(e);
                targetBlock = null;
            }
            if (targetBlock == null) {
                player.sendMessage(Component.translatable("command.terraform.hpos.no_block"));
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

            // Update the selection
            var changed = selection.selectPrimary(targetBlock, true);
            if (!changed) {
                player.sendMessage(Component.translatable("command.terraform.pos1.already_set"));
            }
        }
    }

    public static final class HPos2 extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public HPos2(@Nullable CommandCondition condition) {
            super("hpos2", "tf:hpos2");
            setCondition(condition);

            setDefaultExecutor(this::handleSelectionUpdate);
            addSyntax(this::handleSelectionUpdate, selectionArg);
        }

        private void handleSelectionUpdate(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            // Determine the target position
            Point targetBlock;
            try {
                targetBlock = player.getTargetBlockPosition(512); //todo could be an option somewhere maybe
            } catch (NullPointerException e) {
                if (!e.getMessage().contains("Unloaded chunk"))
                    throw new RuntimeException(e);
                targetBlock = null;
            }
            if (targetBlock == null) {
                player.sendMessage(Component.translatable("command.terraform.hpos.no_block"));
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

            // Update the selection
            var changed = selection.selectSecondary(targetBlock, true);
            if (!changed) {
                player.sendMessage(Component.translatable("command.terraform.pos2.already_set"));
            }
        }
    }

    public static final class Sel extends Command {
        public Sel(@Nullable CommandCondition condition) {
            super("sel", "tf:sel");
            setCondition(condition);

            addSubcommand(new Type());
            addSubcommand(new Clear());
        }

        public static final class Type extends Command {
            public Type() {
                super("type");

                setDefaultExecutor((sender, context) -> sender.sendMessage("Usage blah blah")); //todo

                for (var type : Region.Type.values()) {
                    addSubcommand(new Simple(type));
                }
            }

            private static final class Simple extends Command {
                private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

                private final Region.Type regionType;

                public Simple(@NotNull Region.Type regionType) {
                    super(regionType.name().toLowerCase());
                    this.regionType = regionType;

                    setDefaultExecutor(this::handleSelectRegionType);
                    addSyntax(this::handleSelectRegionType, selectionArg);
                }

                private void handleSelectRegionType(@NotNull CommandSender sender, @NotNull CommandContext context) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.translatable("command.terraform.only_players"));
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

                    // Update the selection
                    selection.setType(regionType);
                    sender.sendMessage(Component.translatable("command.terraform.sel.type.set", Component.text(regionType.name().toLowerCase())));
                }
            }

        }

        public static final class Clear extends Command {
            private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

            public Clear() {
                super("clear");

                setDefaultExecutor(this::handleClear);
                addSyntax(this::handleClear, selectionArg);
            }

            private void handleClear(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("command.terraform.only_players"));
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

                selection.clear();
            }
        }
    }

    public static final class Outset extends Command {

        private final Argument<String> directionModArg = ArgumentType.String("direction-mod");
        private final Argument<Integer> amountArg = ArgumentType.Integer("amount");
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");
        public Outset(@Nullable CommandCondition condition) {
            super("outset", "tf:outset");
            setCondition(condition);

            setDefaultExecutor(this::handleOutset);
            addSyntax(this::handleOutset, amountArg);
            addSyntax(this::handleOutset, amountArg, selectionArg);
            addSyntax(this::handleOutset, directionModArg, amountArg);
            addSyntax(this::handleOutset, directionModArg, amountArg, selectionArg);
            directionModArg.setSuggestionCallback((sender, context, suggestion) -> {
                // TODO: Suggest -h/-v
            });
        }

        private void handleOutset(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            int amount;
            if (context.has(amountArg)) {
                amount = context.get(amountArg);
            } else {
                // TODO: Display syntax of command
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

            if (context.has(directionModArg)) {
                String mod = context.get(directionModArg);
                if (mod.length() != 2) {
                    // TODO: send invalid mod argument
                    return;
                }
                if (mod.charAt(0) == '-') {
                    if (mod.charAt(1) == 'h') {
                        selection.changeSize(amount, false, true);
                    } else if (mod.charAt(1) == 'v') {
                        selection.changeSize(amount, true, false);
                    } else {
                        // TODO: send invalid mod argument
                    }
                    return;
                }
            }
            selection.changeSize(amount, true, true);
        }
    }
    public static final class Inset extends Command {

        private final Argument<String> directionModArg = ArgumentType.String("direction-mod");
        private final Argument<Integer> amountArg = ArgumentType.Integer("amount");
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");
        public Inset(@Nullable CommandCondition condition) {
            super("inset", "tf:inset");
            setCondition(condition);

            setDefaultExecutor(this::handleInset);
            addSyntax(this::handleInset, amountArg);
            addSyntax(this::handleInset, amountArg, selectionArg);
            addSyntax(this::handleInset, directionModArg, amountArg);
            addSyntax(this::handleInset, directionModArg, amountArg, selectionArg);
            directionModArg.setSuggestionCallback((sender, context, suggestion) -> {
                // TODO: Suggest -h/-v
            });
        }

        private void handleInset(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            int amount;
            if (context.has(amountArg)) {
                amount = context.get(amountArg);
            } else {
                // TODO: Display syntax of command
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

            if (context.has(directionModArg)) {
                String mod = context.get(directionModArg);
                if (mod.length() != 2) {
                    // TODO: send invalid mod argument
                    return;
                }
                if (mod.charAt(0) == '-') {
                    if (mod.charAt(1) == 'h') {
                        selection.changeSize(-amount, false, true);
                    } else if (mod.charAt(1) == 'v') {
                        selection.changeSize(-amount, true, false);
                    } else {
                        // TODO: send invalid mod argument
                    }
                    return;
                }
            }
            selection.changeSize(-amount, true, true);
        }
    }
}
