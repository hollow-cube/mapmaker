package net.hollowcube.command;

import net.minestom.server.command.CommandSender;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CommandCondition {

    /**
     * Allows the command execution
     */
    int ALLOW = 0;
    /**
     * Denies the command execution flat out, ending execution here.
     */
    int DENY = 1;
    /**
     * Hides the command, essentially pretending it doesn't exist.
     * It cannot be executed, and it will not show up for completions.
     */
    int HIDE = 2;

    static @NotNull CommandCondition nosuggestion() {
        return (sender, context) -> context.pass() == CommandContext.Pass.SUGGEST ? HIDE : ALLOW;
    }

    static @NotNull CommandCondition and(@NotNull CommandCondition... conditions) {
        return (sender, context) -> {
            for (var condition : conditions) {
                var result = condition.test(sender, context);
                if (result != ALLOW) {
                    return result;
                }
            }
            return ALLOW;
        };
    }

    static @NotNull CommandCondition or(@NotNull CommandCondition... conditions) {
        return (sender, context) -> {
            for (var condition : conditions) {
                var result = condition.test(sender, context);
                if (result == ALLOW) {
                    return result;
                }
            }
            return DENY;
        };
    }

    @MagicConstant(valuesFromClass = CommandCondition.class)
    int test(@NotNull CommandSender sender, @NotNull CommandContext context);
}
