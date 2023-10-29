package net.hollowcube.terraform.give_me_new_home.helper;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.condition.CommandCondition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AliasCommand extends Command {
    private final String target;

    public AliasCommand(@Nullable CommandCondition condition, @NotNull String target, @NotNull String name, @Nullable String... aliases) {
        super(name, aliases);
        setCondition(condition);
        this.target = target;

        setDefaultExecutor((sender, context) -> MinecraftServer.getCommandManager().execute(sender, target));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull AliasCommand addSyntax(@NotNull Argument<?>... args) {
        addSyntax(this::arbitraryForwardingHandler, args);
        return this;
    }

    private void arbitraryForwardingHandler(@NotNull CommandSender sender, @NotNull CommandContext context) {
        var args = context.getInput().split(" ", 2);
        var allArgs = args.length > 1 ? args[1] : "";
        MinecraftServer.getCommandManager().execute(sender, target + " " + allArgs);
    }
}
