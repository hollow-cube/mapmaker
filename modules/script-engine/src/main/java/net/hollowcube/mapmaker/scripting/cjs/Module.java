package net.hollowcube.mapmaker.scripting.cjs;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.util.Garbage;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a common-js module in the script engine.
 */
public class Module {
    private final ScriptEngine engine;
    private final URI uri;
    private final String name;
    private final Map<String, Object> globals = new LinkedHashMap<>();
    private final Map<String, Object> extraModules;

    private Value exports;

    @ApiStatus.Internal
    public Module(@NotNull ScriptEngine engine, @NotNull URI uri, @NotNull String code, @NotNull Map<String, Object> globals, @NotNull Map<String, Object> extraModules) {
        this.engine = engine;
        this.uri = uri;
        this.name = extractFileName(uri);
        this.globals.putAll(globals);
        this.extraModules = Map.copyOf(extraModules);

        this.exports = engine.context().eval("js", "({})");
    }

    public @NotNull URI uri() {
        return uri;
    }

    public @NotNull String filename() {
        return uri.getPath();
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
        return this.exports;
    }

    public @NotNull Map<String, Object> globals() {
        return this.globals;
    }

    public @NotNull Map<String, Object> extraModules() {
        return this.extraModules;
    }

    @ApiStatus.Internal
    public void loadModuleText(@NotNull String code) {
        this.exports = loadJsModule(code); //todo support json
    }

    private @NotNull Value loadJsModule(@NotNull String code) {
        try (var ignored = engine.makeCurrent()) {
            // We wrap the evaluation in a function to ensure that modules are evaluated in different scopes.
            var wrappedCodeBuilder = new StringBuilder();
            wrappedCodeBuilder.append("(function (exports, require, module, __filename, __dirname");
            if (!this.globals.isEmpty())
                wrappedCodeBuilder.append(", ").append(globals.keySet().stream().collect(Collectors.joining(", ")));
            wrappedCodeBuilder.append(") {").append(code).append("})");
            var source = Source.newBuilder("js", wrappedCodeBuilder, this.name).build();

            var context = engine.context();
            var module = context.eval("js", "({})");
            module.putMember("exports", this.exports);
            module.putMember("filename", this.name);
            module.putMember("id", this.name);

            // Args match wrapped function above.
            final String uriPath = this.uri.getPath();
            final int lastSlash = uriPath.lastIndexOf('/');
            final String dirname = lastSlash == -1 ? "" : uriPath.substring(0, lastSlash);
            Object[] args = new Object[5 + this.globals.size()];
            args[0] = exports;
            args[1] = (ProxyExecutable) this::moduleRequire;
            args[2] = module;
            args[3] = this.name;
            args[4] = dirname;
            int i = 5;
            for (var entry : this.globals.entrySet()) {
                args[i++] = entry.getValue();
            }
            context.eval(source).execute(args);

            // Refetch exports because it can be totally overwritten (ie module.exports = function() { ... } is valid).
            return module.getMember("exports");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull Value moduleRequire(@NotNull Value... args) {
        // We have an implementation of require from within this module because the target path needs to be resolved from here.
        // For now we just delegate immediately to the engine loading internal files.

        if (args.length != 1)
            throw new IllegalArgumentException("require() must be called with exactly one argument");
        if (!args[0].isString())
            throw new IllegalArgumentException("require() argument must be a string");
        final String module = args[0].asString();

        final Object extraModule = this.extraModules.get(module);
        if (extraModule != null) return Value.asValue(extraModule);

        final URI loadUri;
        if (module.startsWith(".")) {
            // TODO: appending .js might have to be part of engine.load resolution if it should support json also. not sure
            loadUri = uri.resolve(module + ".js");
        } else {
            loadUri = URI.create("internal:///react/" + args[0].asString() + ".js");
        }

        var globals = new HashMap<>(this.globals);
        globals.put("__hollowcube_moduleId", loadUri.toString());
        return engine.load(loadUri, globals, this.extraModules, (code) -> {
            if (loadUri.getScheme().equals("internal")) return code;
            // language=JS
            return String.format(Garbage.REACT_REFRESH_MODULE_TEMPLATE, code);
        }).exports();
    }

    private static @NotNull String extractFileName(@NotNull URI uri) {
        final String[] pathParts = uri.getPath().split("/");
        return pathParts[pathParts.length - 1];
    }

}
