package net.hollowcube.command.suggestion;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;

public class SuggestionContext implements CommandContext {

    private final CommandSender sender;

    private final Map<String, String> argRawValues = new HashMap<>();
    private final Map<String, Object> argValues = new HashMap<>();

    public SuggestionContext(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public Pass pass() {
        return Pass.SUGGEST;
    }

    @Override
    public CommandSender sender() {
        return sender;
    }

    @Override
    public @UnknownNullability String getRaw(Argument<?> arg) {
        return argRawValues.get(arg.id());
    }

    @Override
    public <T> @UnknownNullability T get(Argument<T> arg) {
        if (argValues.containsKey(arg.id())) {
            //noinspection unchecked
            return (T) argValues.get(arg.id());
        } else {
            return arg.getDefaultValue(sender);
        }
    }

    @Override
    public boolean has(Argument<?> arg) {
        return arg.isOptional() || argValues.containsKey(arg.id());
    }

    public void setArgValue(String argId, String rawValue, Object value) {
        argRawValues.put(argId, rawValue);
        argValues.put(argId, value);
    }
}
