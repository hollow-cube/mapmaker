package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

final class RootCommandNode extends CommandNode {

    @Override
    @NotNull
    CommandNode nodeFor(@NotNull Argument<?> argument) {
        throw new UnsupportedOperationException("use register() to manage root node children");
    }

    void register(@NotNull String name, @NotNull CommandNode node) {
        if (this.children == null) this.children = new ArrayList<>();

        for (ArgumentPair(Argument<?> argument, CommandNode unused) : this.children) {
            if (argument.id().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("duplicate child name: " + name);
            }
        }

        this.children.add(new ArgumentPair(Argument.Literal(name), node));
    }

    @Override
    protected @NotNull CommandResult unwrap(@NotNull CommandResult result) {
        // See the comment on the base implementation of this method
        return result;
    }
}
