package net.hollowcube.command.builder;

import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.hollowcube.command.CommandNode;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record CommandEvaluationContext(
        @NotNull CommandSender commandSender,
        @NotNull Object2IntFunction<CommandNode> idMap,
        @NotNull Object2BooleanFunction<CommandNode> hasBeenRegistered,
        @NotNull Consumer<CommandNode> register
) {

    public @Nullable Integer getId(@NotNull CommandNode node) {
        if (hasBeenRegistered.test(node)) {
            return idMap.getInt(node);
        }

        return null;
    }

    public void register(@NotNull CommandNode node) {
        if (!hasBeenRegistered.test(node)) {
            register.accept(node);
        }
    }

}
