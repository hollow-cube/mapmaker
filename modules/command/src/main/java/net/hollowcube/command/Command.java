package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentOptional;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;

public abstract class Command {
    private final String name;

    // Map of lower cased command name to subcommand.
    private final Map<String, Command> subcommands = new HashMap<>();
    private final List<Syntax> syntaxes = new ArrayList<>();
    private CommandExecutor defaultExecutor = null;

    // Documentation bits
    protected String description = null;
    protected List<String> examples = null;

    public Command(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String name() {
        return name;
    }

    public Map<String, Command> getSubcommands() {
        return subcommands;
    }

    public void addSubcommand(@NotNull Command command) {
        var name = command.name().toLowerCase(Locale.ROOT);
        Check.argCondition(name.isEmpty(), "Subcommand name cannot be empty.");
        Check.argCondition(subcommands.containsKey(name), "Subcommand with name " + name + " already exists.");
        subcommands.put(name, command);
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

    protected @NotNull CommandDoc doc(@NotNull CommandSender sender) {
        boolean allOptional = defaultExecutor != null;
        var simpleArgs = new ArrayList<CommandDoc.Argument>();
        for (var syntax : syntaxes) {
            if (syntax.condition != null) {
                //todo
                var eval = syntax.condition.test(sender, new CommandContext() {
                    @Override
                    public @NotNull Pass pass() {
                        return Pass.SUGGEST;
                    }

                    @Override
                    public @NotNull CommandSender sender() {
                        return sender;
                    }

                    @Override
                    public <T> @UnknownNullability T get(@NotNull Argument<T> arg) {
                        return null;
                    }
                });
                if (eval != CommandCondition.ALLOW) continue;
            }

            // Add the syntax args to the list
            for (var arg : syntax.args) {
                simpleArgs.add(new CommandDoc.Argument(
                        arg.id(), allOptional || arg instanceof ArgumentOptional<?>,
                        arg.defaultName(), arg.description()
                ));
            }
        }
        return new CommandDoc(name, description, simpleArgs, examples);
    }

    protected static @NotNull CommandExecutor playerOnly(@NotNull CommandExecutor.Player executor) {
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
        for (var syntax : syntaxes) {
            if (syntax.condition != null) return false;
        }

        return true;
    }

    record Syntax(
            @Nullable CommandCondition condition,
            @NotNull CommandExecutor executor,
            @NotNull List<Argument<?>> args
    ) {

        public boolean allowsEmpty() {
            if (args.isEmpty()) return true;
            for (var arg : args) {
                if (!(arg instanceof ArgumentOptional<?>)) return false;
            }
            return true;
        }
    }

}
