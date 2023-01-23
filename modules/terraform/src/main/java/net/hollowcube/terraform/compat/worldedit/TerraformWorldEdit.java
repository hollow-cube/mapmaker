package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.terraform.compat.worldedit.command.GeneralCommands;
import net.hollowcube.terraform.compat.worldedit.command.RegionCommands;
import net.hollowcube.terraform.util.AliasCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.arguments.ArgumentType;
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
        commands.register(new AliasCommand("tf:pos1", "/pos1")
                .addSyntax(ArgumentType.RelativeVec3("coordinates")));
        commands.register(new AliasCommand("tf:pos2", "/pos2")
                .addSyntax(ArgumentType.RelativeVec3("coordinates")));
        commands.register(new AliasCommand("tf:hpos1", "/hpos1"));
        commands.register(new AliasCommand("tf:hpos2", "/hpos2"));

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
