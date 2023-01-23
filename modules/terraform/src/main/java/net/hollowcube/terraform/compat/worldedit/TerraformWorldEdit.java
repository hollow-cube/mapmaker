package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.terraform.compat.worldedit.command.GeneralCommands;
import net.hollowcube.terraform.compat.worldedit.command.RegionCommands;
import net.hollowcube.terraform.compat.worldedit.command.SelectionCommands;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TerraformWorldEdit {
    private TerraformWorldEdit() {}

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        var commands = MinecraftServer.getCommandManager();

        // General
        commands.register(new GeneralCommands.Undo());
        commands.register(new GeneralCommands.Redo());

        // Selection
        commands.register(new SelectionCommands.Pos1(condition));
        commands.register(new SelectionCommands.Pos2(condition));
        commands.register(new SelectionCommands.HPos1(condition));
        commands.register(new SelectionCommands.HPos2(condition));

        // Region
        commands.register(new RegionCommands.Set(condition));

        // Generation

        // Schematic/Clipboard

        // Tool

        // Brush

        // Biome

        // Chunk

        // Utility
    }

}
