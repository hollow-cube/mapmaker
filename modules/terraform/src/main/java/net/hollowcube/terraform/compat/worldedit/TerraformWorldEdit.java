package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.terraform.compat.worldedit.command.ClipboardCommands;
import net.hollowcube.terraform.compat.worldedit.command.HistoryCommands;
import net.hollowcube.terraform.compat.worldedit.command.RegionCommands;
import net.hollowcube.terraform.compat.worldedit.command.SelectionCommands;
import net.hollowcube.terraform.compat.worldedit.wand.WandHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TerraformWorldEdit {

    public static void init() {
        init(MinecraftServer.getGlobalEventHandler(), null);
        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addChild(WandHandler.EVENT_NODE);
    }

    public static void init(@NotNull EventNode<? super InstanceEvent> eventNode, @Nullable CommandCondition commandCondition) {
        var commands = MinecraftServer.getCommandManager();
        new SelectionCommands(commands, commandCondition);
        new RegionCommands(commands, commandCondition);
        new HistoryCommands(commands, commandCondition);
        new ClipboardCommands(commands, commandCondition);

        eventNode.addChild(WandHandler.EVENT_NODE);
    }
}
