package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.Module;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * JSX 'implements' the global JSX namespace, really just as a filter to React lib calls.
 *
 * <p>This class should be instantiated once per {@link org.graalvm.polyglot.Context} as it
 * stores some context-local state.</p>
 *
 * @param react The global react module. This is not a public module for scripts to use.
 */
public record JSX(@NotNull Module react) implements ProxyObject {
    private static final Set<String> ALLOWED_APIS = Set.of("createElement", "Fragment", "Suspense");

    @Override
    public Object getMember(@NotNull String key) {
        return ALLOWED_APIS.contains(key) ? react.exports().getMember(key) : null;
    }

    @Override
    public Object getMemberKeys() {
        return ALLOWED_APIS.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        return ALLOWED_APIS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("putMember() not supported.");
    }
}
