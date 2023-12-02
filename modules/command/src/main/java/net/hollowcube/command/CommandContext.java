package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public interface CommandContext {
    /**
     * Which pass of command execution that is.
     *
     * <p>BUILD is when a command packet is being created.</p>
     * <p>SUGGEST is when command suggestions are being computed. It is also used when generating {@link CommandDoc}s</p>
     * <p>EXECUTE is when the command is actually being executed by a sender.</p>
     */
    enum Pass {
        BUILD, SUGGEST, EXECUTE
    }

    @NotNull Pass pass();

    @NotNull CommandSender sender();

    @UnknownNullability String getRaw(@NotNull Argument<?> arg);

    <T> @UnknownNullability T get(@NotNull Argument<T> arg);

    boolean has(@NotNull Argument<?> arg);

    static @NotNull CommandContext fake(@NotNull CommandSender sender) {
        return new CommandContext() {
            @Override
            public @NotNull Pass pass() {
                return Pass.SUGGEST;
            }

            @Override
            public @NotNull CommandSender sender() {
                return sender;
            }

            @Override
            public @UnknownNullability String getRaw(@NotNull Argument<?> arg) {
                return null;
            }

            @Override
            public <T> @UnknownNullability T get(@NotNull Argument<T> arg) {
                return null;
            }

            @Override
            public boolean has(@NotNull Argument<?> arg) {
                return false;
            }
        };
    }
}
