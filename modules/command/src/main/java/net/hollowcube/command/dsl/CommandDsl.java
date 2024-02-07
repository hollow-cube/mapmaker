package net.hollowcube.command.dsl;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandDsl {
    private final String name;
    private final List<String> aliases;

    private CommandCondition condition = null;
    private List<CommandDsl> subcommands = null;
    private List<Syntax> syntaxes = null;

    // Documentation bits
    protected String category = null;
    protected String description = null;
    protected List<String> examples = null;

    public CommandDsl(@NotNull String name, @NotNull String... aliases) {
        this.name = name;
        this.aliases = List.of(aliases);
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<String> aliases() {
        return aliases;
    }

    public void build(@NotNull CommandBuilder builder) {
        if (condition != null) {
            builder.condition(condition);
        }

        if (subcommands != null) {
            for (var subcommand : subcommands) {
                builder.child(subcommand.name(), child -> {
                    subcommand.build(child);

                    for (var alias : subcommand.aliases()) {
                        builder.child(alias, aliasBuilder -> aliasBuilder.redirect(child.node()));
                    }
                });
            }
        }

        if (syntaxes != null) {
            for (var syntax : syntaxes) {
                builder.executes(syntax.executor(), syntax.args());
            }
        }
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

    public void addSyntax(@NotNull CommandExecutor executor, @NotNull Argument<?>... args) {
        if (syntaxes == null) syntaxes = new ArrayList<>();
        syntaxes.add(new Syntax(null, executor, args));
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

    private record Syntax(@Nullable CommandCondition condition, @NotNull CommandExecutor executor,
                          @NotNull Argument<?>[] args) {
    }

}
