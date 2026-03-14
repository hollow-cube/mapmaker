package net.hollowcube.command;

import net.minestom.server.command.CommandSender;
import org.intellij.lang.annotations.MagicConstant;

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

    static CommandCondition nosuggestion() {
        return (_, context) -> context.pass() == CommandContext.Pass.SUGGEST ? HIDE : ALLOW;
    }

    static CommandCondition hideOnClient() {
        return (_, context) -> context.pass() == CommandContext.Pass.BUILD ? HIDE : ALLOW;
    }

    static CommandCondition and(CommandCondition... conditions) {
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

    static CommandCondition or(CommandCondition... conditions) {
        return (sender, context) -> {
            for (var condition : conditions) {
                var result = condition.test(sender, context);
                if (result == ALLOW) {
                    return result;
                }
            }
            return HIDE;
        };
    }

    @MagicConstant(valuesFromClass = CommandCondition.class)
    int test(CommandSender sender, CommandContext context);
}
