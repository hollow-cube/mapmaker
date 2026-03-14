package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;

import java.util.ArrayList;
import java.util.List;

final class RootCommandNode extends CommandNode {

    @Override
    CommandNode nodeFor(Argument<?> argument) {
        throw new UnsupportedOperationException("use register() to manage root node children");
    }

    void register(String name, CommandNode node) {
        if (this.children == null) this.children = new ArrayList<>();

        for (var pair : List.copyOf(this.children)) {
            if (pair.argument().id().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("duplicate child name: " + name);
            }
        }

        this.children.add(new ArgumentPair(Argument.Literal(name), node));
    }

    void unregister(String name) {
        if (this.children == null) return;

        var iter = this.children.iterator();
        while (iter.hasNext()) {
            var pair = iter.next();
            if (pair.argument().id().equalsIgnoreCase(name)) {
                iter.remove();
                return;
            }
        }
    }
}
