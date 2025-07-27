package net.hollowcube.command.dsl;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.util.CommandCategory;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandDsl {
    private final String name;
    private final List<String> aliases;
    // Documentation bits
    protected CommandCategory category = CommandCategory.DEFAULT;
    protected String description = null;
    protected List<String> examples = null;
    private CommandCondition condition = null;
    private List<CommandDsl> subcommands = null;
    private List<Syntax> syntaxes = null;

    public CommandDsl(@NotNull String name, @NotNull String... aliases) {
        this.name = name;
        this.aliases = List.of(aliases);
    }

    public static @NotNull CommandExecutor playerOnly(@NotNull CommandExecutor.PlayerOnly executor) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can execute this command."); //todo pluggable message I guess
                return;
            }

            executor.execute(player, context);
        };
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<String> aliases() {
        return aliases;
    }

    public void build(@NotNull CommandBuilder builder) {
        builder.category(category);
        builder.description(description);
        builder.examples(examples);

        if (condition != null) {
            builder.condition(condition);
        }

        if (subcommands != null) {
            for (var subcommand : subcommands) {
                try {
                    builder.child(subcommand.name(), child -> {
                        subcommand.build(child);

                        for (var alias : subcommand.aliases()) {
                            builder.child(alias, aliasBuilder -> aliasBuilder.redirect(child.node()));
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException("failed to register subcommand " + subcommand.name(), e);
                }
            }
        }

        if (syntaxes != null) {
            for (var syntax : syntaxes) {
                if (syntax.executor != null) {
                    builder.executes(syntax.executor(), syntax.args());
                }
                if (syntax.onSuggestion != null) {
                    builder.suggestion(syntax.onSuggestion, syntax.args);
                }
            }
        }
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    public @Nullable CommandCondition getCondition() {
        return condition;
    }

    public void setCondition(@Nullable CommandCondition condition) {
        this.condition = condition;
    }

    public void addSubcommand(@NotNull CommandDsl command) {
        if (subcommands == null) subcommands = new ArrayList<>();
        subcommands.add(command);
    }

    public void addSuggestionSyntax(@NotNull CommandExecutor onSuggestion, @NotNull Argument<?>... args) {
        addSyntax(null, onSuggestion, args);
    }

    public void addSyntax(@NotNull CommandExecutor executor, @NotNull Argument<?>... args) {
        addSyntax(executor, null, args);
    }

    public void addSyntax(@Nullable CommandExecutor executor, @Nullable CommandExecutor onSuggestion, @NotNull Argument<?>... args) {
        if (syntaxes == null) syntaxes = new ArrayList<>();
        syntaxes.add(new Syntax(null, executor, onSuggestion, args));
        syntaxes.sort((a, b) -> Integer.compare(b.args.length, a.args.length));
    }

    private record Syntax(@Nullable CommandCondition condition, @Nullable CommandExecutor executor,
                          @Nullable CommandExecutor onSuggestion,
                          @NotNull Argument<?>[] args) {
    }

}
