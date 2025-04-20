package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.scripting.util.ContextWrapper;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
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
    public Module(@NotNull ScriptEngine engine, @NotNull URI uri, @NotNull Map<String, Object> globals, @NotNull Map<String, Object> extraModules) {
        this.engine = engine;
        this.uri = uri;
        this.name = extractFileName(uri);
        this.globals.putAll(globals);
        this.extraModules = Map.copyOf(extraModules);

        this.exports = engine.makeCurrent(ContextWrapper::newObject);
    }

    public @NotNull URI uri() {
        return uri;
    }

    public @NotNull String filename() {
        return uri.getPath();
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
        try (var context = engine.makeCurrent()) {
            // We wrap the evaluation in a function to ensure that modules are evaluated in different scopes.
            var wrappedCodeBuilder = new StringBuilder();
            wrappedCodeBuilder.append("(function (exports, require, module, __filename, __dirname");
            if (!this.globals.isEmpty())
                wrappedCodeBuilder.append(", ").append(globals.keySet().stream().collect(Collectors.joining(", ")));
            wrappedCodeBuilder.append(") {").append(code).append("})");
            var source = Source.newBuilder("js", wrappedCodeBuilder, this.name).build();

            var module = context.newObject();
            module.putMember("exports", this.exports);
            module.putMember("filename", this.name);
            module.putMember("id", this.uri.toString());

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
            loadUri = URI.create("%s://%s".formatted(uri.getScheme(), uri.resolve(module + ".js").getPath()));
        } else {
            loadUri = URI.create("internal:///react/" + args[0].asString() + ".js");
        }

        return engine.load(loadUri, this.globals, this.extraModules).exports();
    }

    private static @NotNull String extractFileName(@NotNull URI uri) {
        final String[] pathParts = uri.getPath().split("/");
        return pathParts[pathParts.length - 1];
    }

}
