package net.hollowcube.command.util;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandNode;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public interface CommandReflection {

    @UnknownNullability CommandNode xpath(@NotNull String path, boolean followRedirects);

    @NotNull Collection<Map.Entry<String, CommandNode>> commands(@NotNull CommandSender sender, boolean includeAliases);

    @NotNull Collection<Map.Entry<Argument<?>, CommandNode>> children(@NotNull CommandNode node, @NotNull CommandSender sender);

    void edit(@NotNull CommandNode node, @NotNull Consumer<CommandBuilder> func);

}
