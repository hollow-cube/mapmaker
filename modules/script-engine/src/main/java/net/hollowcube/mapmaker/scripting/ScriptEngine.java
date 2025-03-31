package net.hollowcube.mapmaker.scripting;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.scripting.gui.GuiManager;
import net.hollowcube.mapmaker.scripting.instrumentation.ModuleInterceptor;
import net.hollowcube.mapmaker.scripting.loader.FileSystemLoader;
import net.hollowcube.mapmaker.scripting.loader.InternalScriptLoader;
import net.hollowcube.mapmaker.scripting.loader.ScriptLoader;
import net.hollowcube.mapmaker.scripting.node.Globals;
import net.hollowcube.mapmaker.scripting.node.Process;
import net.hollowcube.mapmaker.scripting.util.ContextWrapper;
import net.minestom.server.instance.Instance;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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
 *
 * <p>ScriptEngine and Module together implement commonjs module loading with the same mechanisms
 * and behaviors as nodejs when in doubt (for example, recursive module loading returning partial
 * results).</p>
 */
public class ScriptEngine {
    public final Env env;
    public final Instance instance;
    public final Globals globals = new Globals(this);

    private final Context context;

    private final Map<String, ScriptLoader> loaders = new HashMap<>();
    private final Map<URI, Module> moduleCache = new HashMap<>();
    private ModuleInterceptor interceptor = null;

    // Sub handlers, created lazily when needed.
    private GuiManager guiManager = null; // Lazy

    public ScriptEngine(@NotNull Instance instance) {
        this.env = new Env(ServerRuntime.getRuntime().isDevelopment());
        this.instance = instance;

        this.context = Context.newBuilder().build();

        try {
            // We always add the internal loader right now, but actually importing React is potentially a privileged action
            // and should be done through a different mechanism.
            this.loaders.put("internal", new InternalScriptLoader("/third_party"));
            // This is also of course temporary, need to figure out how this will be included in dev vs prod.
            if (env().isDevelopment()) {
                this.loaders.put("guilib", new FileSystemLoader("guilib", Path.of("./modules/script-engine/builtin/dist/gui"), this::handleModuleHotSwap));
            } else {
                this.loaders.put("guilib", new InternalScriptLoader("/builtin/gui"));
            }

        } catch (IOException e) {
            throw new RuntimeException("failed to create default script loaders", e);
        }

        setupGlobals();
    }

    public @NotNull Env env() {
        return this.env;
    }

    public @NotNull GuiManager guiManager() {
        if (this.guiManager == null)
            this.guiManager = new GuiManager(this);
        return this.guiManager;
    }

    /**
     * Enters the context on the current thread, returning a closeable that will leave the
     * context. Intended to be used with try-with-resources expressions.
     *
     * @return a closeable that will leave the context when closed
     */
    public @NotNull ContextWrapper makeCurrent() {
        return new ContextWrapper(context);
    }

    public <T> T makeCurrent(@NotNull Function<ContextWrapper, T> func) {
        try (var ctx = makeCurrent()) {
            return func.apply(ctx);
        }
    }

    public @NotNull Module load(@NotNull URI script) {
        return load(script, Map.of(), Map.of());
    }

    public @NotNull Module load(@NotNull URI script, @NotNull Map<String, Object> globals, @NotNull Map<String, Object> extraModules) {
        final Module existing = this.moduleCache.get(script);
        if (existing != null) return existing;

        String code;
        final ScriptLoader loader = this.loaders.get(script.getScheme());
        if (loader == null) {
            throw new UnsupportedOperationException("unsupported uri scheme: " + script.getScheme());
        }

        try {
            code = Objects.requireNonNull(loader.load(script), () -> "module not found: " + script);
        } catch (IOException e) {
            throw new RuntimeException(e); //todo better handling
        }

        // Apply any instrumentation logic to the module.
        if (this.interceptor != null) {
            code = this.interceptor.defineModule(script, code);
        }

        // Insert the module into the cache _before_ trying to evaluate it to handle circular references.
        // This will result in circular references being resolved even if they resolve to partial exports.
        // We are inheriting this behavior from nodejs.
        final Module loaded = new Module(this, script, globals, extraModules);
        this.moduleCache.put(script, loaded);

        // Evaluate the module now that it can be resolved circularly.
        loaded.loadModuleText(code);

        return loaded;
    }

    public void instrument(@NotNull ModuleInterceptor interceptor) {
        // In the future we probably need to support multiple instruments
        // for now i (matt) will remain lazy and have one.
        if (this.interceptor != null) throw new UnsupportedOperationException("only one interceptor supported");
        this.interceptor = interceptor;
    }

    private void setupGlobals() {
        var global = context.getBindings("js");
        global.putMember("process", Process.sandboxedProcess());
        global.putMember("setTimeout", (ProxyExecutable) globals::setTimeout);
        global.putMember("clearTimeout", (ProxyExecutable) globals::clearTimeout);
    }

    private void handleModuleHotSwap(@NotNull URI moduleUri, @Nullable String newCode) {
        var removed = this.moduleCache.remove(moduleUri);
        if (removed == null) return; // Not loaded currently, ignore.
        if (newCode == null) return; // Unload, no need to reload.

        load(moduleUri, removed.globals(), removed.extraModules());
    }

}
