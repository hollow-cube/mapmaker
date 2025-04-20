package net.hollowcube.mapmaker.scripting.util;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public record ContextWrapper(@NotNull Context unsafe) implements AutoCloseable {

    public ContextWrapper {
        unsafe.enter();
    }

    public @NotNull Value globalThis() {
        return unsafe.getBindings("js");
    }

    public @NotNull Value newObject() {
        return unsafe.eval("js", "({})");
    }

    public @NotNull Value eval(@NotNull String code) {
        return unsafe.eval("js", code);
    }

    public @NotNull Value eval(@NotNull Source code) {
        return unsafe.eval(code);
    }

    @Override
    public void close() {
        unsafe.leave();
    }
}
