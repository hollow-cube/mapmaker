package net.hollowcube.luau.ap.tree;

import org.jetbrains.annotations.NotNull;

public interface Visitor<P> {

    default void visit(@NotNull Node node, P param) {
        node.accept(this, param);
    }

    default void visitType(@NotNull Node.Type node, P param) {
        for (Node.Property property : node.properties()) {
            property.accept(this, param);
        }
        for (Node.Method method : node.methods()) {
            method.accept(this, param);
        }
    }

    default void visitProperty(@NotNull Node.Property node, P param) {
    }

    default void visitMethodActual(@NotNull Node.Method.Actual node, P param) {
    }

    default void visitMethodOverload(@NotNull Node.Method.Overload node, P param) {
        for (Node.Method.Actual actual : node.overloads()) {
            actual.accept(this, param);
        }
    }

}
