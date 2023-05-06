package net.hollowcube.terraform.compat.worldedit;

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
    private TerraformWorldEdit() {
    }

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        var commands = MinecraftServer.getCommandManager();

        // General
        commands.register(new AliasCommand(condition, "tf:undo", "/undo")
                .addSyntax(ArgumentType.Integer("count").min(1).setDefaultValue(1)));
        commands.register(new AliasCommand(condition, "tf:redo", "/redo")
                .addSyntax(ArgumentType.Integer("count").min(1).setDefaultValue(1)));
        commands.register(new AliasCommand(condition, "tf:clearhistory", "/clearhistory"));

        // Selection
        commands.register(new AliasCommand(condition, "tf:pos1", "/pos1")
                .addSyntax(ArgumentType.RelativeVec3("coordinates")));
        commands.register(new AliasCommand(condition, "tf:pos2", "/pos2")
                .addSyntax(ArgumentType.RelativeVec3("coordinates")));
        commands.register(new AliasCommand(condition, "tf:hpos1", "/hpos1"));
        commands.register(new AliasCommand(condition, "tf:hpos2", "/hpos2"));

        // Region
        commands.register(new RegionCommands.Set(condition));

        // Generation

        // Schematic/Clipboard
        commands.register(new AliasCommand(condition, "tf:copy", "/copy"));
        commands.register(new AliasCommand(condition, "tf:paste", "/paste"));

        // Tool

        // Brush

        // Biome

        // Chunk

        // Utility
    }

}
