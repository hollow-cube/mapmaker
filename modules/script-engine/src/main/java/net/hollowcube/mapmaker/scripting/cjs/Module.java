package net.hollowcube.mapmaker.scripting.cjs;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a common-js module in the script engine.
 */
public class Module {
    private final ScriptEngine engine;

    public Module(ScriptEngine engine) {
        this.engine = engine;
    }

    /**
     * Returns whether this module is privileged. Privileged modules can only be directly imported from other
     * privileged modules. This is a loose security feature as privileged modules can incorrectly declare
     * globals accessible from other modules. However `require('some_privileged_module')` will not work from
     * unprivileged modules.
     *
     * @return whether this module is privileged
     */
    public boolean privileged() {
        throw new UnsupportedOperationException("todo");
    }

    public @NotNull Value exports() {
        throw new UnsupportedOperationException("todo");
    }

}
