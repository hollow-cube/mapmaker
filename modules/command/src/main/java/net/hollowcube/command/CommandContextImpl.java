package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

final class CommandContextImpl implements CommandContext {
    private final Pass pass;
    private final CommandSender sender;
    private final StringReader reader;

    private final List<Command> commands = new ArrayList<>();
    private final List<Command.Syntax> syntaxes = new ArrayList<>();
    private final List<Argument<?>> args = new ArrayList<>();
    private final List<Integer> argMarks = new ArrayList<>();
    // null indicates that the argument isnt present
    private final List<Maybe<Object>> argValues = new ArrayList<>();
    private final List<String> rawArgStrings = new ArrayList<>();
    private CommandExecutor overrideExecutor = null;

    public CommandContextImpl(@NotNull Pass pass, @NotNull CommandSender sender, @NotNull StringReader reader) {
        this.pass = pass;
        this.sender = sender;
        this.reader = reader;
    }

    @Override
    public @NotNull Pass pass() {
        return pass;
    }

    @Override
    public @NotNull CommandSender sender() {
        return sender;
    }

    @Override
    public @UnknownNullability String getRaw(@NotNull Argument<?> arg) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).id().equals(arg.id())) {
                return rawArgStrings.get(i);
            }
        }

        return null;
    }

    @Override
    public <T> @UnknownNullability T get(@NotNull Argument<T> arg) {
        var index = getArgIndex(arg);
        if (index < 0) return null;

        var maybeValue = argValues.get(index);
        if (maybeValue == null) {
            return null;
        }

        Object value = maybeValue.value();
        if (value instanceof Argument.DeferredValue<?> deferred) {
            value = deferred.get();
            argValues.set(index, new Maybe<>(value));
        }

        //noinspection unchecked
        return (T) value;
    }

    @Override
    public boolean has(@NotNull Argument<?> arg) {
        var index = getArgIndex(arg);
        return index >= 0 && argValues.get(index) != null;
    }

    private int getArgIndex(@NotNull Argument<?> arg) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).id().equals(arg.id())) {
                return i;
            }
        }

        return -1;
    }

    // Internal API

    public @NotNull StringReader reader() {
        return reader;
    }

    public boolean hasCommand() {
        return !commands.isEmpty();
    }

    public boolean isFailed() {
        if (!syntaxes.isEmpty()) return false;
        // If the command has a default executor, it is never a failure.
        return commands.isEmpty() || commands.get(commands.size() - 1).defaultExecutor() == null;
    }

    public @NotNull CommandExecutor executor() {
        assert !isFailed() : "command failed";
        if (overrideExecutor != null) return overrideExecutor;
        if (!syntaxes.isEmpty()) return syntaxes.get(syntaxes.size() - 1).executor();

        var defaultExecutor = commands.get(commands.size() - 1).defaultExecutor();
        assert defaultExecutor != null : "command has no default executor";
        return defaultExecutor;
    }

    // Mark/Restore

    record Mark(int reader, int command, int syntaxes, int args, int argMarks, int argValeus, int rawArgStrings,
                CommandExecutor overrideExecutor) {
    }

    public @NotNull Mark mark() {
        return new Mark(reader.mark(), commands.size(), syntaxes.size(), args.size(), argMarks.size(), argValues.size(), rawArgStrings.size(), overrideExecutor);
    }

    public void restore(@NotNull Mark mark) {
        reader.restore(mark.reader);
        while (commands.size() > mark.command) {
            commands.remove(commands.size() - 1);
        }
        while (syntaxes.size() > mark.syntaxes) {
            syntaxes.remove(syntaxes.size() - 1);
        }
        while (args.size() > mark.args) {
            args.remove(args.size() - 1);
        }
        while (argMarks.size() > mark.argMarks) {
            argMarks.remove(argMarks.size() - 1);
        }
        while (argValues.size() > mark.argValeus) {
            argValues.remove(argValues.size() - 1);
        }
        while (rawArgStrings.size() > mark.rawArgStrings) {
            rawArgStrings.remove(rawArgStrings.size() - 1);
        }
        this.overrideExecutor = mark.overrideExecutor;
    }

    // Other API

    public void pushCommand(@NotNull Command command) {
//        System.out.println("PUSH COMMAND: " + command.name());
        this.commands.add(command);
    }

    public @NotNull Command command() {
        assert !this.commands.isEmpty() : "no command";
        return this.commands.get(this.commands.size() - 1);
    }

    public void pushSyntax(@NotNull Command.Syntax syntax) {
//        System.out.println("PUSH SYNTAX: " + (syntax.condition() != null ? "? " : "") + String.join(" ", syntax.args().stream().map(Object::toString).toList()));
        this.syntaxes.add(syntax);
    }

    public void pushArg(@NotNull Argument<?> arg) {
//        System.out.println("PUSH ARG: " + arg);
        this.args.add(arg);
        this.argMarks.add(this.reader.mark());
    }

    public @Nullable Argument<?> resetToLastArg() {
        if (this.args.isEmpty()) return null;
        this.reader.restore(this.argMarks.get(this.argMarks.size() - 1));
        return this.args.get(this.args.size() - 1);
    }

    public void pushArgValue(@NotNull String raw, @Nullable Maybe<Object> value, @Nullable CommandExecutor overrideExecutor) {
//        System.out.println("PUSH ARG VALUE: " + value);
        this.rawArgStrings.add(raw);
        this.argValues.add(value);
        this.overrideExecutor = overrideExecutor;
    }

    public void setOverrideExecutor(CommandExecutor overrideExecutor) {
        this.overrideExecutor = overrideExecutor;
    }

    public List<Object> argValues() {
        return argValues.stream().map(v -> {
            if (v == null) return null;
            return v.value();
        }).toList();
    }

    record Maybe<T>(T value) {
        public boolean isPresent() {
            return value != null;
        }
    }
}
