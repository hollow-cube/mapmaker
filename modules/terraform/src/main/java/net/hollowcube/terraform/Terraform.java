package net.hollowcube.terraform;

import net.hollowcube.terraform.command.*;
import net.hollowcube.terraform.command.argument.ExtraArguments;
import net.hollowcube.terraform.mask.script.MaybeMask;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Terraform {
    private Terraform() {
    }

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        var commands = MinecraftServer.getCommandManager();

        // Root/Debug
        commands.register(new TerraformCommand());

        // Selection
        commands.register(new SelectionCommands.Pos1(condition));
        commands.register(new SelectionCommands.Pos2(condition));
        commands.register(new SelectionCommands.HPos1(condition));
        commands.register(new SelectionCommands.HPos2(condition));
        commands.register(new SelectionCommands.Sel(condition));
        commands.register(new SelectionCommands.Outset(condition));
        commands.register(new SelectionCommands.Inset(condition));
        commands.register(new SelectionCommands.Chunk(condition));
        commands.register(new SelectionCommands.Size(condition));

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
        commands.register(new ClipboardCommands.Rotate(condition));
        commands.register(new ClipboardCommands.Flip(condition));
        commands.register(new ClipboardCommands.ClearClipboard(condition));

        // Schematic
        commands.register(new SchematicCommand(condition));

        // Testing
        var mask = new Command("mask");
        var maskArg = ExtraArguments.Mask("mask");
        mask.addSyntax((sender, context) -> {
            var maybeMask = context.get(maskArg);
            if (maybeMask instanceof MaybeMask.Error error) {
                var rawText = context.getRaw(maskArg);
                if (rawText.startsWith("\""))
                    rawText = rawText.substring(1);
                if (rawText.endsWith("\""))
                    rawText = rawText.substring(0, rawText.length() - 1);

                var msg = error.error().toFriendlyMessage(rawText);
                for (var line : msg) {
                    sender.sendMessage(line);
                }
            } else {
                sender.sendMessage("Mask is ok");
            }
//            sender.sendMessage("test " + context.get("mask") + " " + context.get("abc"));
        }, maskArg);
        commands.register(mask);
    }
}
