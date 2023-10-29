package net.hollowcube.terraform.give_me_new_home;

import net.hollowcube.terraform.give_me_new_home.helper.ExtraArguments;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class SelectionCommands {
    private SelectionCommands() {
    }

    public static final class Outset extends Command {
        private final Argument<String> directionModArg = ArgumentType.Word("direction-mod").from("all", "horizontal", "vertical");
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
        }

        private void handleOutset(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
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
                String mod = context.get(directionModArg).toLowerCase(Locale.ROOT);
                switch (mod) {
                    case "all" -> selection.changeSize(amount, true, true);
                    case "horizontal" -> selection.changeSize(amount, false, true);
                    case "vertical" -> selection.changeSize(amount, true, false);
                    default ->
                            sender.sendMessage(Component.translatable("command.generic.invalid_argument", Component.text(directionModArg.getId())));
                }
            } else {
                selection.changeSize(amount, true, true);
            }
        }
    }

    public static final class Inset extends Command {
        private final Argument<String> directionModArg = ArgumentType.Word("direction-mod").from("all", "horizontal", "vertical");
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
        }

        private void handleInset(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
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
                String mod = context.get(directionModArg).toLowerCase(Locale.ROOT);
                switch (mod) {
                    case "all" -> selection.changeSize(amount, true, true);
                    case "horizontal" -> selection.changeSize(amount, false, true);
                    case "vertical" -> selection.changeSize(amount, true, false);
                    default ->
                            sender.sendMessage(Component.translatable("command.generic.invalid_argument", Component.text(directionModArg.getId())));
                }
            } else {
                selection.changeSize(amount, true, true);
            }
        }
    }

    public static final class Chunk extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Chunk(@Nullable CommandCondition condition) {
            super("chunk", "tf:chunk");
            setCondition(condition);

            setDefaultExecutor(this::handleChunk);
            addSyntax(this::handleChunk, selectionArg);
        }

        private void handleChunk(@NotNull CommandSender sender, @NotNull CommandContext context) {
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

            int yMin = player.getInstance().getDimensionType().getMinY();
            int yMax = player.getInstance().getDimensionType().getMaxY();
            int x1 = player.getPosition().chunkX() << 4; // Multiply by 16
            int x2 = x1 + net.minestom.server.instance.Chunk.CHUNK_SIZE_X;
            int z1 = player.getPosition().chunkZ() << 4; // Multiply by 16
            int z2 = z1 + net.minestom.server.instance.Chunk.CHUNK_SIZE_Z;
            selection.selectPrimary(new Pos(x1, yMin, z1), true);
            selection.selectSecondary(new Pos(x2, yMax, z2), true);
        }
    }

    public static final class Size extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");
        private final ArgumentWord clipboardArg = ArgumentType.Word("clipboard").from("clipboard");

        public Size(@Nullable CommandCondition condition) {
            super("size", "tf:size");
            setCondition(condition);

            setDefaultExecutor(this::handleSize);
            addSyntax(this::handleSize, selectionArg);
            addSyntax(this::handleSize, clipboardArg); // TODO: Might not be the best practice, but I would have to add a suggestion entry to selectionArg with "clipboard" somehow
        }

        private void handleSize(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            if (context.has(clipboardArg)) {
                if (context.get(clipboardArg).toLowerCase(Locale.ROOT).equals("clipboard")) {
                    var playerSession = PlayerSession.forPlayer(player);
//                    int count = playerSession.clipboard().blocks().length;
//                    sender.sendMessage(Component.translatable("command.terraform.selection.count", Component.text("clipboard"), Component.text(count)));
                }
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
            Region region = selection.region();
            if (region != null) {
                sender.sendMessage(Component.translatable("command.terraform.selection.count", Component.text("selection"), Component.text(region.volume())));
            }
        }
    }
}
