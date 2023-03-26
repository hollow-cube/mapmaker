package net.hollowcube.canvas.mock;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class MockController {
//    private final MockElement

    public <V extends View> @NotNull V show(@NotNull Function<Context, V> viewProvider) {
        return null;
    }

    /**
     * Selects an element by its selector. A selector can have two modes: id and type, and can be made up of many selectors.
     * <p>
     * ID selectors are prefixed with a #, and type selectors are prefixed with a .
     * <p>
     * For example, the selector "#foo" will select a top level var with the ID "foo".
     * <p>
     * The selector ".foo" will select the first component with the type "foo". An @ allows specification of an nth component.
     * For example ".foo@5" will select the 5th component with the type "foo".
     * <p>
     * todo finish this doc & implement it
     */
    public <T extends MockElement> @NotNull T select(@NotNull String selector, @NotNull Class<T> type) {
        if (selector.isBlank()) {

        } else if (selector.startsWith("#")) {
            return root
        } else {
            throw new IllegalArgumentException("Invalid selector: " + selector);
        }
    }

}
