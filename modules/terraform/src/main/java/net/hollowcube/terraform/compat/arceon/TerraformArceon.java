package net.hollowcube.terraform.compat.arceon;

import net.hollowcube.terraform.compat.arceon.command.LoftCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TerraformArceon {

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        var commands = MinecraftServer.getCommandManager();

        commands.register(new LoftCommand(condition));
    }


}
