package net.hollowcube.terraform;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.TerraformCommand;
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
    public @NotNull Set<TerraformStorage> storageTypes() {
        return Set.of(new TerraformStorageMemory());
    }

    @Override
    public @NotNull Set<CommandDsl> commands() {
        return Set.of(
                // Root/Debug
                new TerraformCommand()

                // Selection
//                new PosCommand.Primary.class, PosCommand.Secondary(),
//                new HPosCommand.Primary.class, HPosCommand.Secondary(),
//                new SelCommand(),
//                new SelectionCommands.Outset(condition),
//                new SelectionCommands.Inset(condition),
//                new SelectionCommands.Chunk(condition),
//                new SelectionCommands.Size(condition),

                // Region
//                new SetCommand.class, ReplaceCommand(),
//                new StackCommand.class, SmearCommand(),
//                new MoveCommand(),
//                new SetLightCommand(),

                // History
//                new UndoCommand.class, RedoCommand(),
//                new ClearHistoryCommand(),

                // Clipboard
//                new CopyCommand.class, CutCommand.class, PasteCommand(),
//                new ClipboardCommand(),

                // Schematic
//                new SchemCommand(),

                // Tools
//                new ToolCommand()
        );
    }
}
