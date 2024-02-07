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
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The base terraform module, which provides the default registry assets.
 */
final class BaseModule implements TerraformModule {

    @Override
    public @NotNull Set<RegionSelector.Factory> regionTypes() {
        return Set.of(CuboidRegionSelector.FACTORY);
    }

    @Override
    public @NotNull Set<Class<? extends TerraformStorage>> storageTypes() {
        return Set.of(TerraformStorageMemory.class);
    }

    @Override
    public @NotNull Set<Class<? extends CommandDsl>> commands() {
        return Set.of(
                // Root/Debug
                TerraformCommand.class,

                // Selection
                PosCommand.Primary.class, PosCommand.Secondary.class,
                HPosCommand.Primary.class, HPosCommand.Secondary.class,
                SelCommand.class,
//                new SelectionCommands.Outset(condition),
//                new SelectionCommands.Inset(condition),
//                new SelectionCommands.Chunk(condition),
//                new SelectionCommands.Size(condition),

                // Region
                SetCommand.class, ReplaceCommand.class,
                StackCommand.class, SmearCommand.class,
                MoveCommand.class,
                SetLightCommand.class,

                // History
                UndoCommand.class, RedoCommand.class,
                ClearHistoryCommand.class,

                // Clipboard
                CopyCommand.class, CutCommand.class, PasteCommand.class,
                ClipboardCommand.class,

                // Schematic
                SchemCommand.class,

                // Tools
                ToolCommand.class
        );
    }
}
