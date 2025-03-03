package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.Module;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.instrumentation.ModuleInterceptor;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class ReactRefresh {

    public static void setup(@NotNull ScriptEngine engine) {
        if (!engine.env().isDevelopment()) {
            throw new IllegalStateException("React refresh should only be enabled in development mode.");
        }

        try (var context = engine.makeCurrent()) {
            final Value globalThis = context.globalThis();

            // When react-refresh is loaded then things start to fail without a global window declared.
            // Not sure why this happens its probably worth looking into because this is pretty gross.
            // TODO: ^^^
            globalThis.putMember("window", globalThis);

            // Load react-refresh runtime _before_ any other react imports. It must be done before for it to work.
            final Module reactRefreshRuntime = engine.load(URI.create("internal:///react/react-refresh/runtime.js"));

            // Notably this injects into the global namespace which means we leak react details to other scripts. I think this is bad and we
            // instead should handle this with the Module implementation (ie call this with a new object and expose all the properties added
            // to child modules so it never ends up in the context global scope).
            // TODO: ^^^
            reactRefreshRuntime.exports().invokeMember("injectIntoGlobalHook", globalThis);
            globalThis.putMember("$RefreshReg$", context.newObject());
            globalThis.putMember("$RefreshSig$", context.eval("() => type => type"));

            // Finally, we need to set up instrumentation in module loads for react-refresh to work.
            engine.instrument(new InstrumentationImpl());
        }
    }

    private static class InstrumentationImpl implements ModuleInterceptor {
        // language=JS
        public static final String MODULE_TEMPLATE = """
                var prevRefreshReg = window.$RefreshReg$;
                var prevRefreshSig = window.$RefreshSig$;
                var RefreshRuntime = require('react-refresh/runtime');
                
                window.$RefreshReg$ = (type, id) => {
                  const fullId = module.id + ' ' + id;
                  RefreshRuntime.register(type, fullId);
                }
                window.$RefreshSig$ = RefreshRuntime.createSignatureFunctionForTransform;
                
                try {
                
                    %s
                
                } finally {
                  window.$RefreshReg$ = prevRefreshReg;
                  window.$RefreshSig$ = prevRefreshSig;
                }
                
                // TODO: this needs to be done conditionally on whether the module exclusively exports react components.
                //  We should have an equivalent of module.hot.accept() or whatever the original call is that
                //  prevents it from doing a full 'page' reload: basically like
                //  if (isReactRefreshBoundary(myExports)) {
                //     module.hot.accept(); // Depends on your bundler
                //     RefreshRuntime.performReactRefresh();
                //   }
                //  see: https://github.com/facebook/react/issues/16604#issuecomment-528663101
                //  the metro implementation of `isReactRefreshBoundary` is here:
                //  https://github.com/facebook/metro/blob/febdba2383113c88296c61e28e4ef6a7f4939fda/packages/metro/src/lib/polyfills/require.js#L748-L774
                RefreshRuntime.performReactRefresh();
                """;

        @Override
        public @NotNull String defineModule(@NotNull URI uri, @NotNull String code) {
            if (uri.getScheme().equals("internal")) return code;
            return MODULE_TEMPLATE.formatted(code);
        }

    }
}
