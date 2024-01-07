package net.hollowcube.command;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface CommandManager2 {


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
    @NotNull Suggestion suggest(@NotNull CommandSender sender, @NotNull String input);

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

    @NotNull DeclareCommandsPacket createCommandPacket(@NotNull Player player);

}
