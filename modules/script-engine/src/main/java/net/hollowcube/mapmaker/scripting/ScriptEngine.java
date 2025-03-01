package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.scripting.cjs.Module;
import net.hollowcube.mapmaker.scripting.gui.GuiManager;
import net.hollowcube.mapmaker.scripting.node.Process;
import net.hollowcube.mapmaker.scripting.node.SetTimeout;
import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    private final Map<URI, Module> moduleCache = new HashMap<>();

    private GuiManager guiManager = null; // Lazy

    public ScriptEngine() {
        this.context = Context.newBuilder().build();

        setupGlobals();
    }

    public @NotNull GuiManager guiManager() {
        if (this.guiManager == null)
            this.guiManager = new GuiManager(this);
        return this.guiManager;
    }

    public interface ContextCloser extends AutoCloseable {
        @Override void close();
    }

    public @NotNull Context context() {
        return context;
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

    public @NotNull Module load(@NotNull URI script) {
        return load(script, Map.of(), Map.of());
    }

    public @NotNull Module load(@NotNull URI script, @NotNull Map<String, Object> globals, @NotNull Map<String, Object> extraModules) {
        return this.moduleCache.computeIfAbsent(script, ignored -> {
            final String code = switch (script.getScheme()) {
                case "internal" -> {
                    try (var is = getClass().getResourceAsStream(script.getPath())) {
                        if (is == null) throw new IllegalArgumentException("resource not found: " + script);
                        yield new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "guilib" -> {
                    try {
                        yield Files.readString(Path.of("./guilib/dist/" + script.getPath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case null, default ->
                        throw new UnsupportedOperationException("unsupported uri scheme: " + script.getScheme());
            };
            return new Module(this, script, code, globals, extraModules);
        });
    }

    public @NotNull Module loadText(@NotNull String name, @NotNull String code) {
        var uri = URI.create("file:///tmp/" + name);
        return this.moduleCache.computeIfAbsent(uri, ignored -> new Module(this, uri, code, Map.of(), Map.of()));
    }

    private void setupGlobals() {
        var global = context.getBindings("js");
        global.putMember("process", Process.sandboxedProcess());
        global.putMember("setTimeout", new SetTimeout());
    }

}
