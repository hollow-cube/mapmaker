package net.hollowcube.mapmaker.dev.jsx;

import net.hollowcube.mapmaker.dev.element.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class JSX {
    public static final JSX INSTANCE = new JSX();

    @HostAccess.Export
    public final Object Fragment = FragmentImpl.INSTANCE;

    @HostAccess.Export
    public Value createElement(Value tag, Value props, Value... children) {
        var context = Context.getCurrent();

        if (tag.isHostObject() && tag.asHostObject() == Fragment) { // Fragment
            throw new UnsupportedOperationException("todo");
        }
        if (tag.isString()) { // Primitive
            var node = switch (tag.asString()) {
                case "column" -> new ColumnNode();
                case "row" -> new RowNode();
                case "sprite" -> new SpriteNode();
                case "text" -> new TextNode();
                default -> throw new IllegalArgumentException("Unknown tag: " + tag.asString());
            };
            return context.asValue(node.readProps(props, children));
        }
        if (tag.canExecute()) { // Functional Component
            //todo in the future we would push a new state stack here.

            var inner = assertNode(tag.execute(props));
            return context.asValue(new FCNode(inner));
        }

        throw new IllegalArgumentException("Unknown tag: " + tag);
    }

    private @NotNull List<Node> childrenAsList(@NotNull Value[] children) {
        System.out.println(Arrays.toString(children));
        return Arrays.stream(children)
                .map(Value::asHostObject)
                .map(Node.class::cast)
                .toList();
    }

    private @NotNull Node assertNode(@NotNull Value value) {
        if (!value.isHostObject() || !(value.asHostObject() instanceof Node))
            throw new IllegalArgumentException("Expected jsx, got " + value);
        return value.asHostObject();
    }


    private JSX() {
    }

    private static final class FragmentImpl {
        static final FragmentImpl INSTANCE = new FragmentImpl();

        private FragmentImpl() {
        }

        @Override
        public String toString() {
            return "Fragment";
        }
    }
}
