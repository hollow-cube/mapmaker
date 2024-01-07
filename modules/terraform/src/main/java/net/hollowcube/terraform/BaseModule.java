package net.hollowcube.terraform;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.TerraformCommand;
import net.hollowcube.terraform.command.clipboard.ClipboardCommand;
import net.hollowcube.terraform.command.clipboard.CopyCommand;
import net.hollowcube.terraform.command.clipboard.CutCommand;
import net.hollowcube.terraform.command.clipboard.PasteCommand;
import net.hollowcube.terraform.command.history.ClearHistoryCommand;
import net.hollowcube.terraform.command.history.RedoCommand;
import net.hollowcube.terraform.command.history.UndoCommand;
import net.hollowcube.terraform.command.region.*;
import net.hollowcube.terraform.command.schem.SchemCommand;
import net.hollowcube.terraform.command.selection.HPosCommand;
import net.hollowcube.terraform.command.selection.PosCommand;
import net.hollowcube.terraform.command.selection.SelCommand;
import net.hollowcube.terraform.command.selection.SetLightCommand;
import net.hollowcube.terraform.command.tool.ToolCommand;
import net.hollowcube.terraform.selection.region.CuboidRegionSelector;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.hollowcube.terraform.storage.TerraformStorageMemory;
import net.hollowcube.terraform.tool.ToolHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The base terraform module, which provides the default registry assets.
 */
final class BaseModule implements TerraformModule {
    private final ToolHandler toolHandler = new ToolHandler();

    @Override
    public @NotNull Set<RegionSelector.Factory> regionTypes() {
        return Set.of(CuboidRegionSelector.FACTORY);
    }

    @Override
    public @NotNull Set<TerraformStorage.Factory> storageTypes() {
        return Set.of(TerraformStorageMemory.FACTORY);
    }

    @Override
    public @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        return Set.of(toolHandler.eventNode());
    }

    @Override
    public @NotNull Set<CommandDsl> commands(@NotNull Terraform terraform) {
        return Set.of(
                // Root/Debug
                new TerraformCommand(),

                // Selection
                new PosCommand.Primary(), new PosCommand.Secondary(),
                new HPosCommand.Primary(), new HPosCommand.Secondary(),
                new SelCommand(),
//                new SelectionCommands.Outset(condition),
//                new SelectionCommands.Inset(condition),
//                new SelectionCommands.Chunk(condition),
//                new SelectionCommands.Size(condition),

                // Region
                new SetCommand(terraform), new ReplaceCommand(terraform),
                new StackCommand(), new SmearCommand(),
                new MoveCommand(),
                new SetLightCommand(),

                // History
                new UndoCommand(), new RedoCommand(),
                new ClearHistoryCommand(),

                // Clipboard
                new CopyCommand(), new CutCommand(), new PasteCommand(),
                new ClipboardCommand(),

                // Schematic
                new SchemCommand(),

                // Tools
                new ToolCommand(toolHandler)
        );
    }
}
