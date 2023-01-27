package net.hollowcube.terraform.compat.arceon.command;

import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.selection.region.BezierSurfaceRegionSelector;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoftCommand extends Command {
    public static final String SELECTION = "loft";

    public static @NotNull Selection getSelection(@NotNull Player player) {
        var session = LocalSession.forPlayer(player);
        var selection = session.selection(SELECTION);
        if (selection.type() != Region.Type.BEZIER_SURFACE)
            selection.setType(Region.Type.BEZIER_SURFACE);
        return selection;
    }

    public LoftCommand(@Nullable CommandCondition condition) {
        super("loft", "/loft");
        setCondition(condition);

        addSubcommand(new Frame());
        addSubcommand(new Point());
        addSubcommand(new Remove());
        addSubcommand(new Clear());
        addSubcommand(new Set());

        //todo `loft` (or whatever its named) selection should be locked to a BezierSurfaceRegion
        // it would be weird if your loft commands suddenly stopped working

        //todo these commands are basically just aliases, wonder if i could remove some of the duplicated logic,
        // eg turn //loft clear into just `/tf:sel clear loft`,
        //    //loft frame and point into `/tf:pos1 loft` and `/tf:pos2 loft`
    }

    static class Frame extends Command {
        public Frame() {
            super("frame", "f");

            setDefaultExecutor(this::addFrame);
        }

        private void addFrame(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            var changed = getSelection(player).selectPrimary(player.getPosition(), true);
            if (!changed) {
                sender.sendMessage("blah did not update"); //todo
            }
        }
    }

    static class Point extends Command {
        public Point() {
            super("point", "p");

            setDefaultExecutor(this::addPoint);
        }

        private void addPoint(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            var changed = getSelection(player).selectSecondary(player.getPosition(), true);
            if (!changed) {
                sender.sendMessage("blah did not update"); //todo
            }
        }
    }

    static class Remove extends Command {
        public Remove() {
            super("remove", "r");

            setDefaultExecutor(this::removePoint);
            addSyntax(this::removePoint, ArgumentType.Literal("-c"));
        }

        private void removePoint(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            // getSelection ensures that the selection is a BezierSurfaceRegion
            var selector = (BezierSurfaceRegionSelector) getSelection(player).selector();

            var removed = context.has("-c")
                    ? selector.removePointClosestTo(player.getPosition())
                    : selector.removeLastPoint();
            if (removed) {
                sender.sendMessage("removed a point blah blah");
            } else {
                sender.sendMessage("blah no point to remove blah blah");
            }
        }
    }

    static class Clear extends Command {
        public Clear() {
            super("clear", "c");

            setDefaultExecutor(this::clearSelection);
        }

        private void clearSelection(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            getSelection(player).clear();
            //todo this message should be handled by clear call + an explain flag or something
            sender.sendMessage("Cleared all frames.");
        }
    }

    static class Set extends Command {
        public Set() {
            super("set", "s");


        }
    }

}
