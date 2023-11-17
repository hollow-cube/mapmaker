package net.hollowcube.terraform;

import net.hollowcube.command.CommandManager;
import net.hollowcube.terraform.command.TerraformCommand;
import net.hollowcube.terraform.command.clipboard.ClipboardCommand;
import net.hollowcube.terraform.command.clipboard.CopyCommand;
import net.hollowcube.terraform.command.clipboard.CutCommand;
import net.hollowcube.terraform.command.clipboard.PasteCommand;
import net.hollowcube.terraform.command.history.ClearHistoryCommand;
import net.hollowcube.terraform.command.history.RedoCommand;
import net.hollowcube.terraform.command.history.UndoCommand;
import net.hollowcube.terraform.command.region.ReplaceCommand;
import net.hollowcube.terraform.command.region.SetCommand;
import net.hollowcube.terraform.command.region.SmearCommand;
import net.hollowcube.terraform.command.region.StackCommand;
import net.hollowcube.terraform.command.schem.SchemCommand;
import net.hollowcube.terraform.command.selection.HPosCommand;
import net.hollowcube.terraform.command.selection.PosCommand;
import net.hollowcube.terraform.command.selection.SelCommand;
import net.hollowcube.terraform.command.tool.ToolCommand;
import net.hollowcube.terraform.tool.ToolHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
public final class TerraformOldInit {
    private TerraformOldInit() {
    }

    public static void init(@NotNull CommandManager commandManager, @Nullable EventNode<InstanceEvent> eventNode, @Nullable CommandCondition condition, @NotNull Terraform terraform) {
        if (eventNode == null) {
            eventNode = EventNode.type("terraform", EventFilter.INSTANCE);
            MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        }

        // Root/Debug
        commandManager.register(new TerraformCommand());

        // Selection
        commandManager.register(new PosCommand.Primary());
        commandManager.register(new PosCommand.Secondary());
        commandManager.register(new HPosCommand.Primary());
        commandManager.register(new HPosCommand.Secondary());
        commandManager.register(new SelCommand());
//        commandManager.register(new SelectionCommands.Outset(condition));
//        commandManager.register(new SelectionCommands.Inset(condition));
//        commandManager.register(new SelectionCommands.Chunk(condition));
//        commandManager.register(new SelectionCommands.Size(condition));

        // Region
        commandManager.register(new SetCommand(terraform));
        commandManager.register(new ReplaceCommand(terraform));
        commandManager.register(new StackCommand());
        commandManager.register(new SmearCommand());

        // History
        commandManager.register(new UndoCommand());
        commandManager.register(new RedoCommand());
        commandManager.register(new ClearHistoryCommand());

        // Clipboard
        commandManager.register(new CopyCommand());
        commandManager.register(new CutCommand());
        commandManager.register(new PasteCommand());
        commandManager.register(new ClipboardCommand());

        // Schematic
        commandManager.register(new SchemCommand());

        // Tool
        var toolHandler = new ToolHandler();
        commandManager.register(new ToolCommand(toolHandler));
        eventNode.addChild(toolHandler.eventNode());

    }
}
