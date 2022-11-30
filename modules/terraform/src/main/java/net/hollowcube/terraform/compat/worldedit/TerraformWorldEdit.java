package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.terraform.compat.worldedit.command.ClipboardCommands;
import net.hollowcube.terraform.compat.worldedit.command.HistoryCommands;
import net.hollowcube.terraform.compat.worldedit.command.RegionCommands;
import net.hollowcube.terraform.compat.worldedit.command.SelectionCommands;
import net.hollowcube.terraform.compat.worldedit.wand.WandHandler;
import net.minestom.server.MinecraftServer;

public class TerraformWorldEdit {

    public static void init() {
        var commands = MinecraftServer.getCommandManager();
        new SelectionCommands(commands);
        new RegionCommands(commands);
        new HistoryCommands(commands);
        new ClipboardCommands(commands);

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addChild(WandHandler.EVENT_NODE);
    }

}
