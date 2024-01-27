package net.hollowcube.command.util;

import net.hollowcube.command.CommandNode;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface CommandReflection {

    @NotNull Collection<Map.Entry<String, CommandNode>> commands(@NotNull CommandSender sender, boolean includeAliases);

    @NotNull Collection<Map.Entry<Argument<?>, CommandNode>> children(@NotNull CommandNode node, @NotNull CommandSender sender);

}
