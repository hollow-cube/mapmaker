package net.hollowcube.terraform;

import net.hollowcube.terraform.command.*;
import net.hollowcube.terraform.command.argument.ExtraArguments;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Terraform {
    private Terraform() {}

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        var commands = MinecraftServer.getCommandManager();

        // Selection
        commands.register(new SelectionCommands.Pos1(condition));
        commands.register(new SelectionCommands.Pos2(condition));
        commands.register(new SelectionCommands.HPos1(condition));
        commands.register(new SelectionCommands.HPos2(condition));
        commands.register(new SelectionCommands.Sel(condition));

        // Region
        commands.register(new RegionCommands.Set(condition));
        commands.register(new RegionCommands.Replace(condition));

        // History
        commands.register(new HistoryCommands.Undo(condition));
        commands.register(new HistoryCommands.Redo(condition));
        commands.register(new HistoryCommands.ClearHistory(condition));

        // Clipboard
        commands.register(new ClipboardCommands.Copy(condition));
        commands.register(new ClipboardCommands.Cut(condition));
        commands.register(new ClipboardCommands.Paste(condition));

        // Schematic
        commands.register(new SchematicCommand(condition));

        // Testing
        var mask = new Command("mask");
        mask.addSyntax((sender, context) -> {
            sender.sendMessage("test " + context.get("mask") + " " + context.get("abc"));
        }, ExtraArguments.Mask("mask"));
        commands.register(mask);

    }
}
