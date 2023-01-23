package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SelectionCommands {
    private SelectionCommands() {}

    public static final class Pos1 extends Command {
        private final ArgumentRelativeVec3 coordinatesArg = ArgumentType.RelativeVec3("coordinates");

        public Pos1(@Nullable CommandCondition condition) {
            super("/pos1");
            setCondition(condition);

            setDefaultExecutor(this::setPos1);
            addSyntax(this::setPos1, coordinatesArg);
        }

        public void setPos1(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            Point coordinates;
            if (context.has(coordinatesArg)) {
                coordinates = context.get(coordinatesArg).fromSender(sender);
            } else {
                coordinates = player.getPosition();
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            if (selection.selectPrimary(coordinates)) {
                selection.explainPrimary(coordinates);
            } else {
                player.sendMessage(Component.translatable("command.worldedit.pos1.already_set"));
            }
        }
    }

    public static final class Pos2 extends Command {
        private final ArgumentRelativeVec3 coordinatesArg = ArgumentType.RelativeVec3("coordinates");

        public Pos2(@Nullable CommandCondition condition) {
            super("/pos2");
            setCondition(condition);

            setDefaultExecutor(this::setPos2);
            addSyntax(this::setPos2, coordinatesArg);
        }

        public void setPos2(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            Point coordinates;
            if (context.has(coordinatesArg)) {
                coordinates = context.get(coordinatesArg).fromSender(sender);
            } else {
                coordinates = player.getPosition();
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            if (selection.selectSecondary(coordinates)) {
                selection.explainSecondary(coordinates);
            } else {
                player.sendMessage(Component.translatable("command.worldedit.pos2.already_set"));
            }
        }
    }

    public static final class HPos1 extends Command {
        public HPos1(@Nullable CommandCondition condition) {
            super("/hpos1");
            setCondition(condition);

            setDefaultExecutor(this::setHPos1);
        }

        public void setHPos1(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            Point targetBlock;
            try {
                targetBlock = player.getTargetBlockPosition(512); //todo could be an option somewhere maybe
            } catch (NullPointerException e) {
                if (!e.getMessage().contains("Unloaded chunk"))
                    throw new RuntimeException(e);
                targetBlock = null;
            }
            if (targetBlock == null) {
                player.sendMessage(Component.translatable("command.worldedit.hpos.no_block"));
                return;
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            if (selection.selectPrimary(targetBlock)) {
                selection.explainPrimary(targetBlock);
            } else {
                player.sendMessage(Component.translatable("command.worldedit.pos1.already_set"));
            }
        }
    }

    public static final class HPos2 extends Command {
        public HPos2(@Nullable CommandCondition condition) {
            super("/hpos2");
            setCondition(condition);

            setDefaultExecutor(this::setHPos2);
        }

        public void setHPos2(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.worldedit.only_players"));
                return;
            }

            Point targetBlock;
            try {
                targetBlock = player.getTargetBlockPosition(512); //todo could be an option somewhere maybe
            } catch (NullPointerException e) {
                if (!e.getMessage().contains("Unloaded chunk"))
                    throw new RuntimeException(e);
                targetBlock = null;
            }
            if (targetBlock == null) {
                player.sendMessage(Component.translatable("command.worldedit.hpos.no_block"));
                return;
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            if (selection.selectSecondary(targetBlock)) {
                selection.explainSecondary(targetBlock);
            } else {
                player.sendMessage(Component.translatable("command.worldedit.pos2.already_set"));
            }
        }
    }



/*
    public void size(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        var region = session.getRegionSelector(player.getInstance()).getRegion();

        if (region == null) {
            player.sendMessage("Make a region selection first.");
            return;
        }

        if (region instanceof CuboidRegion cuboid) {
            sender.sendMessage("Type: cuboid");
            sender.sendMessage("Position 1: " + cuboid.pos1());
            sender.sendMessage("Position 2: " + cuboid.pos2());
            sender.sendMessage("Volume: " + cuboid.volume());
            //todo improve me/make region describe itself
        } else {
            player.sendMessage("Only cuboid regions are supported.");
        }

    }

    public void wand(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        boolean added = player.getInventory().addItemStack(ItemStack.of(Material.WOODEN_AXE));
        if (!added) {
            sender.sendMessage("not enough inventory space");
        } else {
            sender.sendMessage("Left click: select Pos #1; Right click: select Pos #2");
        }
    }*/
}
