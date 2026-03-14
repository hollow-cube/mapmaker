package net.hollowcube.command.builder;

import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.hollowcube.command.CommandNode;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record CommandEvaluationContext(
        CommandSender commandSender,
        Object2IntFunction<CommandNode> idMap,
        Object2BooleanFunction<CommandNode> hasBeenRegistered,
        Consumer<CommandNode> register
) {

    public @Nullable Integer getId(CommandNode node) {
        if (hasBeenRegistered.test(node)) {
            return idMap.getInt(node);
        }

        return null;
    }

    public void register(CommandNode node) {
        if (!hasBeenRegistered.test(node)) {
            register.accept(node);
        }
    }

}
