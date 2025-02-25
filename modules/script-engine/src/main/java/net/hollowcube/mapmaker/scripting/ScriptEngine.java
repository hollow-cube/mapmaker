package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.scripting.cjs.Module;
import net.hollowcube.mapmaker.scripting.node.Process;
import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * ScriptEngine is the root of a script execution. It can contain multiple scripts (modules) which
 * have shared global state (such as Symbols in JS).
 *
 * <p>A ScriptEngine maps to a single {@link org.graalvm.polyglot.Context} internally, so limits
 * are applied to an entire script engine not a single script.</p>
 *
 * <p>For scripting in mapmaker we would intend to have one script for global actions, and one
 * script for each player. If we do apply limits and a player engine exceeds those limits the
 * player will be removed from the map. If a global exceeds those limits, the map world itself
 * will be closed with an error.</p>
 */
public class ScriptEngine {
    private final Context context;

    public ScriptEngine() {
        this.context = Context.newBuilder().build();

        setupGlobals();
    }

    public interface ContextCloser extends AutoCloseable {
        @Override void close();
    }

    /**
     * Enters the context on the current thread, returning a closeable that will leave the
     * context. Intended to be used with try-with-resources expressions.
     *
     * @return a closeable that will leave the context when closed
     */
    public @NotNull ContextCloser makeCurrent() {
        context.enter();
        return context::leave;
    }

    /**
     * Loads (or returns from cache) the module at the given URI with privileged permissions.
     * If this module has been loaded in a non-privileged context, this method will throw an
     * exception.
     *
     * @param script The URI of the script.
     * @return
     */
    public @NotNull Module loadPrivileged(@NotNull URI script) {
        throw new UnsupportedOperationException("todo");

    }

    public @NotNull Module load(@NotNull URI script) {
        throw new UnsupportedOperationException("todo");

    }

    private void setupGlobals() {
        var global = context.getBindings("js");
        global.putMember("process", Process.sandboxedProcess());
    }

}
