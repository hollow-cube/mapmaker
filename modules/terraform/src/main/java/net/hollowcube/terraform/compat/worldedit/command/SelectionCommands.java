package net.hollowcube.terraform.compat.worldedit.command;

public final class SelectionCommands {
    private SelectionCommands() {}



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
