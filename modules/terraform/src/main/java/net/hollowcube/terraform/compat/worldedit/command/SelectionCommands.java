package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.compat.worldedit.util.CommandUtil;
import net.hollowcube.terraform.region.CuboidRegion;
import net.hollowcube.terraform.session.Session;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

public class SelectionCommands {

    public SelectionCommands(@NotNull CommandManager commands, @Nullable CommandCondition commandCondition) {
        commands.register(CommandUtil.singleSyntaxCommand("/pos1", this::pos1, commandCondition));
        commands.register(CommandUtil.singleSyntaxCommand("/pos2", this::pos2, commandCondition));
        commands.register(CommandUtil.singleSyntaxCommand("/size", this::size, commandCondition));
        commands.register(CommandUtil.singleSyntaxCommand("/wand", this::wand, commandCondition));
    }

    public void pos1(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        var selector = session.getRegionSelector(player.getInstance());

        var pos = player.getPosition();
        selector.selectPrimary(pos);

        //todo temp
        player.sendMessage(MessageFormat.format("Pos1 set to {0},{1},{2}", pos.blockX(), pos.blockY(), pos.blockZ()));
    }

    public void pos2(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        var selector = session.getRegionSelector(player.getInstance());

        var pos = player.getPosition();
        selector.selectSecondary(pos);

        //todo temp
        player.sendMessage(MessageFormat.format("Pos2 set to {0},{1},{2}", pos.blockX(), pos.blockY(), pos.blockZ()));
    }

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
    }
}
