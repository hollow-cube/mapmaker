package net.hollowcube.command.util;

import net.hollowcube.command.CommandNode;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.Map;

public interface CommandReflection {

    @UnknownNullability CommandNode xpath(String path, boolean followRedirects);

    Collection<Map.Entry<String, CommandNode>> commands(CommandSender sender, boolean includeAliases);

    Collection<Map.Entry<Argument<?>, CommandNode>> children(CommandNode node, CommandSender sender);

}
