package net.hollowcube.terraform;

import net.hollowcube.terraform.command.*;
import net.hollowcube.terraform.command.helper.ExtraArguments;
import net.hollowcube.terraform.mask.script.MaybeMask;
import net.hollowcube.terraform.tool.ToolHandler;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Terraform {
    private Terraform() {
    }

    public static void init(@NotNull CommandManager commandManager, @NotNull EventNode<InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        // Root/Debug
        commandManager.register(new TerraformCommand(condition));

        // Selection
        commandManager.register(new SelectionCommands.Pos1(condition));
        commandManager.register(new SelectionCommands.Pos2(condition));
        commandManager.register(new SelectionCommands.HPos1(condition));
        commandManager.register(new SelectionCommands.HPos2(condition));
        commandManager.register(new SelectionCommands.Sel(condition));
        commandManager.register(new SelectionCommands.Outset(condition));
        commandManager.register(new SelectionCommands.Inset(condition));
        commandManager.register(new SelectionCommands.Chunk(condition));
        commandManager.register(new SelectionCommands.Size(condition));

        // Region
        commandManager.register(new RegionCommands.Set(condition));
        commandManager.register(new RegionCommands.Replace(condition));

        // History
        commandManager.register(new HistoryCommands.Undo(condition));
        commandManager.register(new HistoryCommands.Redo(condition));
        commandManager.register(new HistoryCommands.ClearHistory(condition));

        // Clipboard
        commandManager.register(new ClipboardCommands.Copy(condition));
        commandManager.register(new ClipboardCommands.Cut(condition));
        commandManager.register(new ClipboardCommands.Paste(condition));
        commandManager.register(new ClipboardCommands.ClipboardCommand(condition));

        // Schematic
        commandManager.register(new SchematicCommand(condition));

        // Tool
        var toolHandler = new ToolHandler();
        commandManager.register(new ToolCommand(condition, toolHandler));
        eventNode.addChild(toolHandler.eventNode());

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
        commandManager.register(mask);
    }
}
