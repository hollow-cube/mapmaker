package net.hollowcube.mapmaker.scripting.cjs;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

/**
 * Represents a common-js module in the script engine.
 */
public class Module {
    private final ScriptEngine engine;
    private final URI uri;
    private final String name;

    private final Value exports;

    public Module(@NotNull ScriptEngine engine, @NotNull URI uri, @NotNull String code) {
        this.engine = engine;
        this.uri = uri;
        this.name = extractFileName(uri);

        this.exports = loadJsModule(uri, code);
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

    private @NotNull Value loadJsModule(@NotNull URI uri, @NotNull String code) {
        try (var ignored = engine.makeCurrent()) {
            // We wrap the evaluation in a function to ensure that modules are evaluated in different scopes.
            var wrappedCode = "(function (exports, require, module, __filename, __dirname) {" + code + "})";
            var source = Source.newBuilder("js", wrappedCode, this.name).build();

            var context = engine.context();
            var exports = context.eval("js", "({})");
            var module = context.eval("js", "({})");
            module.putMember("exports", exports);
            module.putMember("filename", this.name);
            module.putMember("id", this.name);
            module.putMember("loaded", false);
            // There is also a concept of module hierarchy, we don't currently do anything with it so dont expose the key.
            // Those keys would be parent & child

            // Args match wrapped function above.
            final String uriPath = this.uri.getPath();
            final int lastSlash = uriPath.lastIndexOf('/');
            final String dirname = lastSlash == -1 ? "" : uriPath.substring(0, lastSlash);
            context.eval(source).execute(exports, (ProxyExecutable) this::moduleRequire, module, this.name, dirname);

            // Refetch exports because it can be totally overwritten (ie module.exports = function() { ... } is valid).
            return module.getMember("exports");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull Value moduleRequire(@NotNull Value... args) {
        // We have an implementation of require from within this module because the target path needs to be resolved from here.
        // For now we just delegate immediately to the engine loading internal files.

        final URI loadUri = URI.create("internal:///third_party/react/" + args[0].asString() + ".js");
        return engine.load(loadUri).exports();
    }

    private static @NotNull String extractFileName(@NotNull URI uri) {
        final String[] pathParts = uri.getPath().split("/");
        return pathParts[pathParts.length - 1];
    }

}
