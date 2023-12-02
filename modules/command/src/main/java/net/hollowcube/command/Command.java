package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class Command {
    private final String name;
    private final List<String> aliases;

    // Map of lower cased command name to subcommand.
    private final Map<String, Command> subcommands = new HashMap<>();
    private final List<Command> uniqueSubcommands = new ArrayList<>();
    private final List<Syntax> syntaxes = new ArrayList<>();
    private CommandExecutor defaultExecutor = null;
    private CommandCondition condition = null;

    // Documentation bits
    protected String description = null;
    protected List<String> examples = null;

    public Command(@NotNull String name, @NotNull String... aliases) {
        this.name = name;
        this.aliases = List.of(aliases);
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<String> aliases() {
        return aliases;
    }

    public Map<String, Command> getSubcommands() {
        return subcommands;
    }

    public @NotNull Collection<Command> getUniqueSubcommands() {
        return uniqueSubcommands;
    }

    public void addSubcommand(@NotNull Command command) {
        var name = command.name().toLowerCase(Locale.ROOT);
        Check.argCondition(name.isEmpty(), "Subcommand name cannot be empty.");
        Check.argCondition(subcommands.containsKey(name), "Subcommand with name " + name + " already exists.");
        for (var alias : command.aliases) {
            Check.argCondition(alias.isEmpty(), "Subcommand alias cannot be empty.");
            Check.argCondition(subcommands.containsKey(alias.toLowerCase(Locale.ROOT)), "Subcommand with alias " + name + " already exists.");
        }

        subcommands.put(name, command);
        for (var alias : command.aliases) {
            subcommands.put(alias.toLowerCase(Locale.ROOT), command);
        }
        uniqueSubcommands.add(command);
    }

    public void setDefaultExecutor(@NotNull CommandExecutor executor) {
        this.defaultExecutor = executor;
    }

    public void addSyntax(@NotNull CommandExecutor executor, @NotNull Argument<?>... args) {
        syntaxes.add(new Syntax(null, executor, List.of(args)));
    }

    public void addSyntax(@NotNull CommandCondition condition, @NotNull CommandExecutor executor, @NotNull Argument<?>... args) {
        syntaxes.add(new Syntax(condition, executor, List.of(args)));
    }

    @Nullable CommandCondition condition() {
        return condition;
    }

    public void setCondition(@Nullable CommandCondition condition) {
        this.condition = condition;
    }

    protected @NotNull CommandDoc doc(@NotNull CommandSender sender) {
        boolean allOptional = defaultExecutor != null;
        var simpleArgs = new ArrayList<CommandDoc.Argument>();
        for (var syntax : syntaxes) {
            if (syntax.condition != null) {
                //todo
                var eval = syntax.condition.test(sender, CommandContext.fake(sender));
                if (eval != CommandCondition.ALLOW) continue;
            }

            // Add the syntax args to the list
            for (var arg : syntax.args) {
                simpleArgs.add(new CommandDoc.Argument(
                        arg.id(), allOptional || arg.isOptional(),
                        arg.defaultName(), arg.description()
                ));
            }
        }
        return new CommandDoc(name, description, simpleArgs, examples);
    }

    public static @NotNull CommandExecutor playerOnly(@NotNull CommandExecutor.Player executor) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can execute this command."); //todo pluggable message I guess
                return;
            }

            executor.execute(player, context);
        };
    }

    @Nullable Command findSubcommand(@NotNull String name) {
        return subcommands.get(name.toLowerCase(Locale.ROOT));
    }

    @NotNull List<Syntax> syntaxes() {
        return syntaxes;
    }

    public @Nullable CommandExecutor defaultExecutor() {
        return defaultExecutor;
    }

    /**
     * Returns true if the command is static, meaning it and none of its children depend on the sender for parsing.
     * <p>Static commands may have significantly more aggressive caching.</p>
     */
    @ApiStatus.Internal
    public boolean isStatic() {
        if (condition != null) return false;
        for (var syntax : syntaxes) {
            if (syntax.condition != null) return false;
        }

        return true;
    }

    public boolean isPlausiblyExecutable() {
        return defaultExecutor != null || !syntaxes.isEmpty();
    }

    record Syntax(
            @Nullable CommandCondition condition,
            @NotNull CommandExecutor executor,
            @NotNull List<Argument<?>> args
    ) {

        public boolean allowsEmpty() {
            if (args.isEmpty()) return true;
            for (var arg : args) {
                if (!arg.isOptional()) return false;
            }
            return true;
        }
    }

}
