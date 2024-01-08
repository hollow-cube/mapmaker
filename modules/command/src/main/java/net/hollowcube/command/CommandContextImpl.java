package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;

public class CommandContextImpl implements CommandContext {
    private final CommandSender sender;

    private final Map<String, String> argRawValues = new HashMap<>();
    private final Map<String, Object> argValues = new HashMap<>();

    public CommandContextImpl(@NotNull CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public @NotNull Pass pass() {
        return Pass.EXECUTE;
    }

    @Override
    public @NotNull CommandSender sender() {
        return sender;
    }

    @Override
    public @UnknownNullability String getRaw(@NotNull Argument<?> arg) {
        return argRawValues.get(arg.id());
    }

    @Override
    public <T> @UnknownNullability T get(@NotNull Argument<T> arg) {
        //noinspection unchecked
        return (T) argValues.get(arg.id());
    }

    @Override
    public boolean has(@NotNull Argument<?> arg) {
        return argValues.containsKey(arg.id());
    }

    public void setArgValue(@NotNull String argId, @NotNull String rawValue, @NotNull Object value) {
        argRawValues.put(argId, rawValue);
        argValues.put(argId, value);
    }
}
