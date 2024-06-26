package net.hollowcube.command;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.CommandReflection;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.*;

import java.util.function.Consumer;

public interface CommandManager {

    default @UnknownNullability CommandNode xpath(@NotNull String path) {
        return xpath(path, true);
    }

    /**
     * XPath is used to search for a command in the graph using a path-like syntax of argument IDs.
     *
     * <p>For example: cmd.a.b.c</p>
     *
     * @param path The path to search for
     * @return The command node, or null if not found
     */
    @UnknownNullability CommandNode xpath(@NotNull String path, boolean followRedirects);

    void register(@NotNull String name, @NotNull CommandNode node);

    void register(@NotNull String name, @NotNull Consumer<CommandBuilder> func);

    void register(@NotNull CommandDsl command);

    /**
     * Returns suggestions for the given command input. The cursor position is always assumed to be
     * at the end of the input string.
     *
     * @param sender The command sender
     * @param input  The input text to provide suggestions for
     * @return The command suggestions
     */
    @NotNull
    Suggestion suggest(@NotNull CommandSender sender, @NotNull String input);

    /**
     * Executes the given command input for the given sender. This function does not indicate success and will not
     * throw any errors on failure. There is no introspection into the result of the execution at this time.
     *
     * <p>This function blocks until the executor is finished running (if one was found), so should be executed from
     * a safe point (ie a virtual thread).</p>
     *
     * @param sender The command sender
     * @param input  The input text to execute
     */
    @Blocking
    @NotNull CommandResult execute(@NotNull CommandSender sender, @NotNull String input);

    @Nullable DeclareCommandsPacket createCommandPacket(@NotNull Player player);

    @Nullable DeclareCommandsPacket createCommandPacketV2(@NotNull Player player);

    /**
     * Provides access to the command manager's reflection API, which can be used to inspect the command graph.
     *
     * <p>Note that this API is relatively unstable and may change over time. The primary use case is
     * {@link net.hollowcube.command.util.HelpCommand}</p>
     *
     * @return The command reflection API
     */
    @ApiStatus.Experimental
    @NotNull CommandReflection reflect();

}
