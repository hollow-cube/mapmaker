package net.hollowcube.mapmaker.scripting.instrumentation;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * A module interceptor can be registered with a ScriptEngine to intercept module loading and
 * execution in order to provide additional functionality.
 */
public interface ModuleInterceptor {

    default @NotNull String defineModule(@NotNull URI uri, @NotNull String code) {
        return code;
    }

}
