package net.hollowcube.command.dsl;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.util.CommandCategory;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandDsl {
    private final String name;
    private final List<String> aliases;
    // Documentation bits
    protected @Nullable CommandCategory category = CommandCategory.DEFAULT;
    protected @Nullable String description = null;
    protected @Nullable List<String> examples = null;
    private @Nullable CommandCondition condition = null;
    private @Nullable List<CommandDsl> subcommands = null;
    private @Nullable List<Syntax> syntaxes = null;

    public CommandDsl(String name, String... aliases) {
        this.name = name;
        this.aliases = List.of(aliases);
    }

    public static CommandExecutor playerOnly(CommandExecutor.PlayerOnly executor) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can execute this command."); //todo pluggable message I guess
                return;
            }

            executor.execute(player, context);
        };
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return aliases;
    }

    public void build(CommandBuilder builder) {
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

    public void setDescription(String description) {
        this.description = description;
    }

    public @Nullable CommandCondition getCondition() {
        return condition;
    }

    public void setCondition(@Nullable CommandCondition condition) {
        this.condition = condition;
    }

    public void addSubcommand(CommandDsl command) {
        if (subcommands == null) subcommands = new ArrayList<>();
        subcommands.add(command);
    }

    public void addSuggestionSyntax(CommandExecutor onSuggestion, Argument<?>... args) {
        addSyntax(null, onSuggestion, args);
    }

    public void addSyntax(CommandExecutor executor, Argument<?>... args) {
        addSyntax(executor, null, args);
    }

    public void addSyntax(@Nullable CommandExecutor executor, @Nullable CommandExecutor onSuggestion, Argument<?>... args) {
        if (syntaxes == null) syntaxes = new ArrayList<>();
        syntaxes.add(new Syntax(null, executor, onSuggestion, args));
        syntaxes.sort((a, b) -> Integer.compare(b.args.length, a.args.length));
    }

    private record Syntax(@Nullable CommandCondition condition, @Nullable CommandExecutor executor,
                          @Nullable CommandExecutor onSuggestion,
                          Argument<?>[] args) {
    }

}
